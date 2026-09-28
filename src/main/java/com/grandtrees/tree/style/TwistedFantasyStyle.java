package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeParams;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.geom.Shapes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Fantasy dark oak: thick spiralling trunk on a wide root flare, long near-horizontal limbs ending
 * in flat pads, glow-berry vines hanging underneath.
 */
public final class TwistedFantasyStyle implements TreeStyle {
    public static final TwistedFantasyStyle INSTANCE = new TwistedFantasyStyle();

    @Override
    public void generate(TreeContext c) {
        TreeParams p = c.params;
        double tr = c.size == TreeSize.SMALL ? 0.5 : p.trunkRadius() * 1.15;
        double trunkH = p.height() * c.rand(0.5, 0.62);

        double turns = c.rand(0.35, 0.7) * (c.random.nextBoolean() ? 1 : -1);
        double phase = c.randAngle();
        double swing = tr * 0.6 + 1.0 + c.size.ordinal() * 0.35;
        Vec3 lean = Shapes.horizontal(c.randAngle()).scale(trunkH * c.rand(0.0, 0.12));
        List<Vec3> trunk = spiral(c, trunkH, turns, phase, swing, lean);
        Shapes.path(c, trunk, tr * 1.2, Math.max(0.5, tr * 0.7), Kind.LOG);
        if (c.size.atLeast(TreeSize.LARGE)) {
            // A second strand wound around the first reads as a twisted double trunk.
            List<Vec3> strand = spiral(c, trunkH * 0.85, turns, phase + Math.PI, swing + tr * 0.6, lean);
            Shapes.path(c, strand, tr * 0.75, 0.6, Kind.LOG);
        }
        Shapes.rootFlare(c, tr, p.roots() + 2, 1.4, false);

        double cr = p.clusterRadius();
        double reach = p.canopyRadius() - cr * 1.2;
        int count = p.branches();
        double a0 = c.randAngle();
        List<Vec3> pads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double ang = a0 + i * TreeContext.TAU / count + c.rand(-0.3, 0.3);
            Vec3 dir = Shapes.horizontal(ang);
            Vec3 s = Shapes.pointAtHeight(trunk, c.base.y + trunkH * c.rand(0.55, 1.0));
            double len = reach * c.rand(0.75, 1.05);
            double rise = len * c.rand(0.05, 0.3);
            Vec3 e = s.add(dir.scale(len)).add(0, rise, 0);
            Vec3 c1 = s.add(dir.scale(len * 0.3)).add(0, len * 0.28, 0);
            Vec3 c2 = s.add(dir.scale(len * 0.7)).add(0, rise + len * 0.12, 0);
            Shapes.bezier(c, s, c1, c2, e, Math.max(0.5, tr * 0.5), 0.5, Kind.LOG);
            pads.add(e);
            if (c.size.atLeast(TreeSize.BIG) && c.random.nextFloat() < 0.6f) {
                pads.add(Shapes.bez(s, c1, c2, e, 0.55).add(0, 0.5, 0));
            }
        }
        for (int i = 0; i < pads.size(); i++) {
            Vec3 e = pads.get(i);
            double k = i % 2 == 0 ? 1.25 : 0.85;
            Shapes.blob(c, e.add(0, 1, 0), cr * k * c.rand(0.9, 1.1), cr * 0.5, cr * k * c.rand(0.9, 1.1), 0.3, 0.35);
        }
        Vec3 top = trunk.get(trunk.size() - 1);
        Shapes.limb(c, top, top.add(0, cr * 0.6, 0), Math.max(0.5, tr * 0.5), 0.5, Kind.LOG);
        Shapes.blob(c, top.add(0, cr * 0.5, 0), cr * 1.35, cr * 0.65, cr * 1.35, 0.35, 0.3);

        Shapes.caveVines(c, 0.05, 2 + c.size.ordinal(), 0.3);
        Shapes.sideVines(c, 0.02, 2 + c.size.ordinal());
    }

    private static List<Vec3> spiral(TreeContext c, double trunkH, double turns, double phase, double swing, Vec3 lean) {
        List<Vec3> pts = new ArrayList<>();
        int n = 8;
        for (int k = 0; k <= n; k++) {
            double t = (double) k / n;
            double ang = phase + turns * TreeContext.TAU * t;
            double amp = swing * Math.pow(t, 0.8);
            pts.add(c.base.add(lean.scale(t)).add(Math.cos(ang) * amp, trunkH * t, Math.sin(ang) * amp));
        }
        return pts;
    }
}
