#include <jni.h>
#include "llama.h"
#include <algorithm>
#include <memory>
#include <mutex>
#include <stdexcept>
#include <string>
#include <vector>

extern "C" JNIEXPORT jbyteArray JNICALL
Java_ru_vladis_realtorai_ai_NativeQwen_generate(JNIEnv* env, jobject, jstring file, jbyteArray input, jobject tokenSink) {
    try {
        static std::once_flag init;
        std::call_once(init, llama_backend_init);
        const char* path = env->GetStringUTFChars(file, nullptr);
        if (!path) return nullptr;
        auto mp = llama_model_default_params();
        mp.n_gpu_layers = 0;
        std::unique_ptr<llama_model, decltype(&llama_model_free)> model(llama_model_load_from_file(path, mp), llama_model_free);
        env->ReleaseStringUTFChars(file, path);
        if (!model) throw std::runtime_error("Не удалось загрузить веса GGUF");
        jmethodID emit = nullptr;
        if (tokenSink) {
            auto sinkClass = env->GetObjectClass(tokenSink);
            emit = env->GetMethodID(sinkClass, "update", "([B)Z");
            env->DeleteLocalRef(sinkClass);
            if (!emit || env->ExceptionCheck()) return nullptr;
        }
        auto update = [&](const std::string& value) {
            if (!emit) return true;
            auto bytes = env->NewByteArray(value.size());
            if (!bytes) return false;
            env->SetByteArrayRegion(bytes, 0, value.size(), reinterpret_cast<const jbyte*>(value.data()));
            bool keepGoing = env->CallBooleanMethod(tokenSink, emit, bytes);
            env->DeleteLocalRef(bytes);
            return keepGoing && !env->ExceptionCheck();
        };
        auto cp = llama_context_default_params();
        cp.n_ctx = 2048;
        cp.n_batch = 256;
        cp.n_threads = cp.n_threads_batch = 4;
        std::unique_ptr<llama_context, decltype(&llama_free)> ctx(llama_init_from_model(model.get(), cp), llama_free);
        if (!ctx) throw std::runtime_error("Недостаточно памяти для контекста модели");
        std::string prompt(env->GetArrayLength(input), '\0');
        env->GetByteArrayRegion(input, 0, prompt.size(), reinterpret_cast<jbyte*>(prompt.data()));
        const auto* vocab = llama_model_get_vocab(model.get());
        std::vector<llama_token> tokens(cp.n_ctx);
        auto n = llama_tokenize(vocab, prompt.data(), prompt.size(), tokens.data(), tokens.size(), false, true);
        if (n <= 0 || n > 1750) throw std::runtime_error("Сократите запрос для локального контекста");
        for (int i = 0; i < n; i += cp.n_batch) {
            if (!update("")) return nullptr;
            if (llama_decode(ctx.get(), llama_batch_get_one(tokens.data()+i, std::min<int>(cp.n_batch, n-i))) != 0)
                throw std::runtime_error("Ошибка обработки запроса");
        }
        std::unique_ptr<llama_sampler, decltype(&llama_sampler_free)> sampler(llama_sampler_init_greedy(), llama_sampler_free);
        std::string answer;
        for (int i = 0; i < 256; i++) {
            auto token = llama_sampler_sample(sampler.get(), ctx.get(), -1);
            if (llama_vocab_is_eog(vocab, token)) break;
            char piece[512];
            auto len = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, false);
            if (len < 0) throw std::runtime_error("Ошибка декодирования токена");
            answer.append(piece, len);
            if ((i + 1) % 8 == 0 && !update(answer)) return nullptr;
            if (llama_decode(ctx.get(), llama_batch_get_one(&token, 1)) != 0)
                throw std::runtime_error("Ошибка генерации ответа");
        }
        auto out = env->NewByteArray(answer.size());
        if (out) env->SetByteArrayRegion(out, 0, answer.size(), reinterpret_cast<const jbyte*>(answer.data()));
        return out;
    } catch (const std::exception& e) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), e.what());
        return nullptr;
    }
}
