package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeParams;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.geom.Shapes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Tall spruce: straight tapered trunk, bare lower section, drooping whorls narrowing to a spire. */
public final class ConiferStyle implements TreeStyle {
    public static final ConiferStyle INSTANCE = new ConiferStyle();

    @Override
    public void generate(TreeContext c) {
        TreeParams p = c.params;
        double tr = p.trunkRadius();
        double H = p.height() * c.rand(1.0, 1.15);
        Vec3 lean = Shapes.horizontal(c.randAngle()).scale(H * c.rand(0.0, 0.06));
        Vec3 top = c.base.add(lean).add(0, H, 0);
        List<Vec3> trunk = List.of(c.base, c.base.add(lean.scale(0.4)).add(0, H * 0.5, 0), top);
        Shapes.path(c, trunk, tr, 0.5, Kind.LOG);
        Shapes.rootFlare(c, tr, p.roots(), 0.9, false);

        double bare = H * (c.size == TreeSize.SMALL ? 0.15 : c.rand(0.18, 0.3));
        double maxLen = p.canopyRadius() * 0.85;
        int spacing = c.size.atLeast(TreeSize.BIG) ? 3 : 2;
        double foliage = 1 + c.size.ordinal() * 0.12;
        double a0 = c.randAngle();
        int tier = 0;
        for (double y = bare; y < H - 2; y += spacing + (c.random.nextFloat() < 0.3f ? 1 : 0)) {
            double rel = (y - bare) / (H - bare);
            double len = Math.max(1.5, maxLen * Math.pow(1 - rel, 0.9) * c.rand(0.85, 1.1));
            int count = c.size == TreeSize.SMALL ? 4 : c.randInt(5, 7);
            Vec3 s = c.base.add(lean.scale(y / H)).add(0, y, 0);
            for (int i = 0; i < count; i++) {
                double ang = a0 + tier * 2.4 + i * TreeContext.TAU / count + c.rand(-0.25, 0.25);
                Vec3 dir = Shapes.horizontal(ang);
                Vec3 mid = s.add(dir.scale(len * 0.5)).add(0, -len * 0.05, 0);
                Vec3 e = s.add(dir.scale(len)).add(0, -len * c.rand(0.15, 0.35), 0);
                double r0 = Math.max(0.5, tr * 0.35);
                Shapes.limb(c, s, mid, r0, 0.5, Kind.LOG);
                Shapes.limb(c, mid, e, 0.5, 0.5, Kind.LOG);
                int n = Math.max(2, (int) Math.ceil(len));
                for (int k = 1; k <= n; k++) {
                    double t = (double) k / n;
                    Vec3 pt = t <= 0.5 ? lerp(s, mid, t * 2) : lerp(mid, e, (t - 0.5) * 2);
                    double rr = Shapes.lerp(2.0, 1.1, t) * foliage;
                    Shapes.blob(c, pt.add(0, 0.3, 0), rr, 0.9, rr, 0.6, 0.35);
                }
            }
            tier++;
        }
        for (double y = H - 3; y <= H + 2; y += 1) {
            double rr = Math.max(0.6, (H + 2 - y) * 0.45);
            Shapes.blob(c, c.base.add(lean).add(0, y, 0), rr, 0.8, rr, 1.0, 0.2);
        }
        Shapes.limb(c, top.add(0, -2, 0), top.add(0, 1, 0), 0.5, 0.5, Kind.LOG);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return new Vec3(Shapes.lerp(a.x, b.x, t), Shapes.lerp(a.y, b.y, t), Shapes.lerp(a.z, b.z, t));
    }
}
