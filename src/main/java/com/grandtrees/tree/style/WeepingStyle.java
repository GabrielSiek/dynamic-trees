package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeParams;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.geom.Shapes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Weeping willow: short (sometimes twin) trunk, arching dome and long leaf/vine curtains. */
public final class WeepingStyle implements TreeStyle {
    public static final WeepingStyle INSTANCE = new WeepingStyle();

    @Override
    public void generate(TreeContext c) {
        TreeParams p = c.params;
        double tr = p.trunkRadius();
        double H = p.height();
        double trunkH = H * c.rand(0.4, 0.5);
        boolean twin = c.size.atLeast(TreeSize.BIG) && c.random.nextFloat() < 0.45f;

        List<Vec3> tops = new ArrayList<>();
        if (twin) {
            double splitH = trunkH * c.rand(0.25, 0.4);
            Vec3 split = c.base.add(0, splitH, 0);
            Shapes.limb(c, c.base, split, tr * 1.1, tr, Kind.LOG);
            double ang = c.randAngle();
            for (int s = 0; s < 2; s++) {
                Vec3 dir = Shapes.horizontal(ang + s * Math.PI + c.rand(-0.3, 0.3));
                double spread = tr + c.rand(1.5, 3.0);
                double up = trunkH - splitH;
                Vec3 t = split.add(dir.scale(spread)).add(0, up, 0);
                Vec3 c1 = split.add(dir.scale(spread * 0.2)).add(0, up * 0.4, 0);
                Vec3 c2 = t.add(dir.scale(-spread * 0.1)).add(0, -up * 0.3, 0);
                Shapes.bezier(c, split, c1, c2, t, tr * 0.75, Math.max(0.5, tr * 0.5), Kind.LOG);
                tops.add(t);
            }
        } else {
            List<Vec3> trunk = StyleUtil.kinkedTrunkPoints(c, trunkH, 1, tr + 0.5);
            Shapes.path(c, trunk, tr * 1.1, Math.max(0.5, tr * 0.7), Kind.LOG);
            tops.add(trunk.get(trunk.size() - 1));
        }
        Shapes.rootFlare(c, tr, p.roots(), 1.1, false);

        double cr = p.clusterRadius();
        double reach = p.canopyRadius() - cr;
        List<Vec3> ends = new ArrayList<>();
        List<Vec3> mids = new ArrayList<>();
        int per = Math.max(3, p.branches() / tops.size() + 1);
        for (Vec3 top : tops) {
            double a0 = c.randAngle();
            for (int i = 0; i < per; i++) {
                Vec3 dir = Shapes.horizontal(a0 + i * TreeContext.TAU / per + c.rand(-0.3, 0.3));
                double len = reach * c.rand(0.7, 1.0);
                double up = (H - trunkH) * c.rand(0.55, 0.9);
                Vec3 e = top.add(dir.scale(len)).add(0, up * 0.55, 0);
                Vec3 c1 = top.add(dir.scale(len * 0.2)).add(0, up, 0);
                Vec3 c2 = top.add(dir.scale(len * 0.7)).add(0, up * 1.05, 0);
                Shapes.bezier(c, top, c1, c2, e, Math.max(0.5, tr * 0.45), 0.5, Kind.LOG);
                ends.add(e);
                mids.add(Shapes.bez(top, c1, c2, e, 0.5));
            }
            Shapes.limb(c, top, top.add(0, (H - trunkH) * 0.7, 0), Math.max(0.5, tr * 0.4), 0.5, Kind.LOG);
            Shapes.blob(c, top.add(0, (H - trunkH) * 0.8, 0), cr * 1.2, cr * 0.75, cr * 1.2, 0.8, 0.3);
        }
        for (Vec3 e : ends) Shapes.blob(c, e, cr * 1.1, cr * 0.7, cr * 1.1, 0.6, 0.3);
        for (Vec3 m : mids) Shapes.blob(c, m, cr * 0.9, cr * 0.6, cr * 0.9, 0.6, 0.3);

        int leafLen = 1 + c.size.ordinal() / 2;
        int vineLen = Math.max(2, (int) (H * 0.4));
        Shapes.leafStrands(c, 0.2, 0.8, leafLen, vineLen, c.canvas.computeLeafDistances());
    }
}
