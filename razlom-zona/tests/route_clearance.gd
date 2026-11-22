extends SceneTree
## Route QA: assert that checkpoint solids do not intersect the player corridor.
## This is a source-scene geometry regression, not an Android FPS or visual test.
const ROUTE_SCRIPT = preload("res://scripts/route.gd")
const CAPSULE_RADIUS := 0.42
const SAFETY_MARGIN := 0.12

func _initialize() -> void:
    _run.call_deferred()

func _run() -> void:
    var route: Node3D = ROUTE_SCRIPT.new()
    root.add_child(route)
    await process_frame
    var points: Array = route.ROUTE_POINTS
    var checked := 0
    for node in route.get_children():
        for body in node.get_children():
            if not (body is StaticBody3D):
                continue
            for collision in body.get_children():
                if not (collision is CollisionShape3D):
                    continue
                if not (collision.shape is BoxShape3D):
                    continue
                var size: Vector3 = collision.shape.size
                var center: Vector3 = node.global_position + body.position + collision.position
                # Only obstacles reaching the player body above the ground matter.
                if center.y + size.y * 0.5 < 0.3:
                    continue
                checked += 1
                for i in range(points.size() - 1):
                    var a: Vector3 = points[i]
                    var b: Vector3 = points[i + 1]
                    # Conservative 2D broad phase: sample the route every 0.2m.
                    var steps := maxi(1, ceili(a.distance_to(b) / 0.2))
                    for j in range(steps + 1):
                        var p: Vector3 = a.lerp(b, float(j) / float(steps))
                        var dx: float = maxf(absf(p.x - center.x) - size.x * 0.5, 0.0)
                        var dz: float = maxf(absf(p.z - center.z) - size.z * 0.5, 0.0)
                        if Vector2(dx, dz).length() < CAPSULE_RADIUS + SAFETY_MARGIN:
                            push_error("Route clearance blocked by %s near %s" % [node.name, p])
                            quit(1)
                            return
    if checked < 4:
        push_error("Too few checkpoint/forest solid obstacles inspected: %d" % checked)
        quit(1)
        return
    if route.get_node_or_null("RaisedBarrier") == null:
        push_error("Raised barrier is missing")
        quit(1)
        return
    print("PASS route clearance: ", checked, " solid boxes checked against ", points.size() - 1, " segments")
    quit()
