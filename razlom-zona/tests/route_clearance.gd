extends SceneTree
## Route QA: assert that checkpoint solids do not intersect the player corridor.
## Uses collider-local coordinates so rotated checkpoint obstacles cannot evade clearance QA.
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
                var center: Vector3 = collision.global_position
                var to_local: Transform3D = collision.global_transform.affine_inverse()
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
                        var local_p: Vector3 = to_local * p
                        var dx: float = maxf(absf(local_p.x) - size.x * 0.5, 0.0)
                        var dz: float = maxf(absf(local_p.z) - size.z * 0.5, 0.0)
                        if Vector2(dx, dz).length() < CAPSULE_RADIUS + SAFETY_MARGIN:
                            push_error("Route clearance blocked by %s near %s" % [node.name, p])
                            quit(1)
                            return
    if checked < 4:
        push_error("Too few checkpoint/forest solid obstacles inspected: %d" % checked)
        quit(1)
        return
    # These are visual guide elements: adding a collider would obstruct the
    # existing player corridor even if the box-only broad-phase missed it.
    var nonblocking: Array[String] = ["RaisedBarrier", "G4Wayfinder", "CheckpointWindow"]
    for i in range(points.size() - 1):
        nonblocking.append("RoadMark_%02d" % i)
    for id in nonblocking:
        var marker := route.get_node_or_null(id)
        if marker == null:
            push_error("Route visual marker missing: " + id)
            quit(1)
            return
        for child in marker.get_children():
            if child is CollisionObject3D or child is CollisionShape3D:
                push_error("Visual marker unexpectedly blocks player: " + id)
                quit(1)
                return
    # Check that the G-4 handoff point remains anchored at the descent.
    if points[points.size() - 1].distance_to(Vector3(24.0, 0.0, -20.0)) > 0.01:
        push_error("G-4 route endpoint moved without updating descent QA")
        quit(1)
        return
    print("PASS route clearance: ", checked, " solid boxes checked against ", points.size() - 1, " segments")
    quit()
