import pathlib
root=pathlib.Path("realtor")
base=root/"app/src/main/java/ru/vladis/realtorai"
def change(path,old,new):
    p=base/path
    s=p.read_text()
    assert old in s, f"Missing expected source: {path}: {old[:60]}"
    p.write_text(s.replace(old,new,1))
change("MainVm.kt",'Room.databaseBuilder(app, AppDb::class.java, "realtor.db").build()','ru.vladis.realtorai.autopilot.CrmDb.get(app)')
change("hunter/HuntWorker.kt",'val s = HuntRun.refreshAll(applicationContext); HuntRun.notifyDeals(applicationContext, s); return Result.success()','val s = HuntRun.refreshAll(applicationContext); if (ru.vladis.realtorai.autopilot.AutopilotEngine.enabled(applicationContext)) ru.vladis.realtorai.autopilot.AutopilotEngine.sync(applicationContext, s.errors); HuntRun.notifyDeals(applicationContext, s); return Result.success()')
change("hunter/HunterVm.kt",'HuntWorker.schedule(ctx, on)','HuntWorker.schedule(ctx, on || ru.vladis.realtorai.autopilot.AutopilotEngine.enabled(ctx))')
change("hunter/HunterVm.kt",'HunterImport.ingest(dao, rows)\n        }.onSuccess','HunterImport.ingest(dao, rows).also { ru.vladis.realtorai.autopilot.AutopilotEngine.syncIfEnabled(ctx) }\n        }.onSuccess')
change("hunter/HunterVm.kt",'HunterImport.ingest(dao, listOf(HunterImport.fromText(text, url)))','HunterImport.ingest(dao, listOf(HunterImport.fromText(text, url))).also { ru.vladis.realtorai.autopilot.AutopilotEngine.syncIfEnabled(ctx) }')
change("hunter/HunterVm.kt",'val s = HuntRun.refreshAll(ctx); val n','val s = HuntRun.refreshAll(ctx); if (ru.vladis.realtorai.autopilot.AutopilotEngine.enabled(ctx)) ru.vladis.realtorai.autopilot.AutopilotEngine.sync(ctx, s.errors); val n')
change("ui/App.kt",'s == "analytics" ->','s == "autopilot" -> ru.vladis.realtorai.autopilot.AutopilotScreen(vm, { sub = "hunter" }, { tab = "crm"; sub = null }, { tab = "tasks"; sub = null }, { sub = null })\n                s == "analytics" ->')
change("ui/App.kt",'{ sub = "sales" }, { sub = "leads" })','{ sub = "sales" }, { sub = "leads" }, { sub = "autopilot" })')
change("ui/Home.kt",'openLeads: () -> Unit) {','openLeads: () -> Unit, openAutopilot: () -> Unit) {')
change("ui/Home.kt",'Title("Рабочее пространство")','Title("Мой рабочий день")')
change("ui/Home.kt",'        item {\n            Column(Modifier.fillMaxWidth()', '''        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF204D45),Color(0xFF26364C)))).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("АВТОПИЛОТ / 01",color=Lime,style=MaterialTheme.typography.labelMedium)
                Text("Меньше рутины. Больше сделок.",color=Color.White,style=MaterialTheme.typography.headlineSmall)
                Text("Импорт объектов, поиск дублей и подбор по бюджету в одном центре.",color=Color(0xFFD1E1DF))
                Button(onClick=openAutopilot,modifier=Modifier.fillMaxWidth()) { Text("Открыть автопилот"); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward,null,Modifier.size(18.dp)) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth()''')
p=root/"app/build.gradle.kts"
s=p.read_text().replace('versionCode = 4; versionName = "0.4"','versionCode = 5; versionName = "0.5"')
p.write_text(s)
