package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeParams;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.geom.Shapes;
import net.minecraft.world.phys.Vec3;

/** Savanna acacia: leaning trunk that forks into wide, low-rising limbs carrying thin canopy pads. */
public final class UmbrellaStyle implements TreeStyle {
    public static final UmbrellaStyle INSTANCE = new UmbrellaStyle();

    @Override
    public void generate(TreeContext c) {
        if (c.size == TreeSize.SMALL) {
            small(c);
            return;
        }
        TreeParams p = c.params;
        double tr = p.trunkRadius();
        // Acacias are wide rather than tall.
        double H = p.height() * 0.75;
        double trunkH = H * c.rand(0.4, 0.5);
        double cr = p.clusterRadius();
        double padR = Math.min(cr * 1.15, p.canopyRadius() * 0.35);
        double padY = 1.2 + c.size.ordinal() * 0.25;
        Vec3 leanDir = Shapes.horizontal(c.randAngle());
        double leanAmt = Math.min(trunkH * c.rand(0.15, 0.3), p.canopyRadius() * 0.2);

        Vec3 fork = c.base.add(leanDir.scale(leanAmt)).add(0, trunkH, 0);
        Vec3 c1 = c.base.add(0, trunkH * 0.45, 0);
        Vec3 c2 = c.base.add(leanDir.scale(leanAmt * 0.6)).add(0, trunkH * 0.8, 0);
        Shapes.bezier(c, c.base, c1, c2, fork, tr, Math.max(0.5, tr * 0.7), Kind.LOG);
        Shapes.rootFlare(c, tr, p.roots(), 0.9, false);

        int limbs = 2 + c.size.ordinal() / 2 + (c.random.nextBoolean() ? 1 : 0);
        double reach = Math.max(2, p.canopyRadius() - padR * 0.6 - leanAmt);
        double span = H - trunkH;
        double a0 = c.randAngle();
        for (int i = 0; i < limbs; i++) {
            double ang = a0 + i * TreeContext.TAU / limbs + c.rand(-0.4, 0.4);
            Vec3 dir = Shapes.horizontal(ang);
            double len = reach * c.rand(0.7, 1.0);
            double rise = span * c.rand(0.35, 0.8);
            Vec3 e = fork.add(dir.scale(len)).add(0, rise, 0);
            Vec3 l1 = fork.add(dir.scale(len * 0.35)).add(0, rise * 0.7, 0);
            Vec3 l2 = fork.add(dir.scale(len * 0.75)).add(0, rise * 0.95, 0);
            Shapes.bezier(c, fork, l1, l2, e, Math.max(0.5, tr * 0.65), 0.5, Kind.LOG);
            pad(c, e, padR * c.rand(0.9, 1.1), padY);

            if (c.size.atLeast(TreeSize.BIG)) {
                Vec3 m = Shapes.bez(fork, l1, l2, e, 0.55);
                Vec3 side = Shapes.horizontal(ang + (c.random.nextBoolean() ? 0.8 : -0.8));
                Vec3 se = m.add(side.scale(len * 0.45)).add(0, rise * 0.1, 0);
                Shapes.limb(c, m, se, 0.5, 0.5, Kind.LOG);
                pad(c, se, padR * 0.75, padY * 0.85);
            }
        }
        Vec3 crown = StyleUtil.leader(c, fork, span * c.rand(0.7, 0.95), tr * 0.5);
        pad(c, crown, padR * 1.1, padY);
        StyleUtil.coverTopWood(c, fork.y);
    }


    /**
     * Small acacia: a short leaning trunk forking into 2-3 limbs, each ending under a full flat
     * canopy (a wide layer with a narrower one on top) that hides the limb ends.
     */
    private static void small(TreeContext c) {
        int trunkH = c.randInt(3, 4);
        Vec3 leanDir = Shapes.horizontal(c.randAngle());
        Vec3 fork = c.base.add(leanDir.scale(c.rand(0.8, 1.6))).add(0, trunkH, 0);
        Vec3 c1 = c.base.add(0, trunkH * 0.5, 0);
        Vec3 c2 = c.base.add(leanDir.scale(0.6)).add(0, trunkH * 0.85, 0);
        Shapes.bezier(c, c.base, c1, c2, fork, 0.5, 0.5, Kind.LOG);

        int limbs = 2 + (c.random.nextFloat() < 0.4f ? 1 : 0);
        double a0 = c.randAngle();
        int canopyY = Shapes.floor(fork.y) + c.randInt(1, 2);
        for (int i = 0; i < limbs; i++) {
            double ang = a0 + i * TreeContext.TAU / limbs + c.rand(-0.4, 0.4);
            double len = c.rand(1.5, 2.6);
            int y = canopyY + (i > 0 && c.random.nextFloat() < 0.35f ? -1 : 0);
            Vec3 e = fork.add(Shapes.horizontal(ang).scale(len));
            e = new Vec3(e.x, y + 0.5, e.z);
            Shapes.limb(c, fork, e, 0.5, 0.5, Kind.LOG);
            double r = c.rand(2.4, 3.0);
            StyleUtil.leafDisc(c, e.x, y, e.z, r, 0.25);
            StyleUtil.leafDisc(c, e.x, y + 1, e.z, r - 1.1, 0.2);
        }
        StyleUtil.coverTopWood(c, fork.y);
    }
    private static void pad(TreeContext c, Vec3 at, double r, double thickness) {
        Shapes.blob(c, at.add(0, thickness * 0.6, 0), r, thickness, r, 0.45, 0.3);
    }
}
