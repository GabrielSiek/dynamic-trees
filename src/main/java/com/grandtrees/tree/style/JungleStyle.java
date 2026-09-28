package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeParams;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.geom.Shapes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Jungle giant: tall trunk on buttress roots, crown at the top, vines and cocoa pods. */
public final class JungleStyle implements TreeStyle {
    public static final JungleStyle INSTANCE = new JungleStyle();

    @Override
    public void generate(TreeContext c) {
        TreeParams p = c.params;
        double tr = p.trunkRadius();
        double H = p.height() * c.rand(1.05, 1.2);
        double trunkH = H * c.rand(0.72, 0.82);
        List<Vec3> trunk = StyleUtil.kinkedTrunkPoints(c, trunkH, 1, 1.0);
        Shapes.path(c, trunk, tr, Math.max(0.5, tr * 0.7), Kind.LOG);
        Shapes.rootFlare(c, tr, p.roots() + 2, 1.3, true);

        double cr = p.clusterRadius();
        double span = Math.max(1, H - trunkH - cr * 0.5);
        List<Vec3> ends = StyleUtil.crownBranches(c, trunk, trunkH, p.branches(), p.canopyRadius() - cr * 1.1,
                0.85, span, 0.05, 0.45, tr * 0.55, c.size.atLeast(TreeSize.BIG), null);
        ends.add(StyleUtil.leader(c, trunk.get(trunk.size() - 1), span * c.rand(0.45, 0.6), tr * 0.5));
        for (Vec3 e : ends) {
            Shapes.blob(c, e.add(0, cr * 0.2, 0), cr * c.rand(1.0, 1.2), cr * 0.6, cr * c.rand(1.0, 1.2), 0.5, 0.35);
        }

        if (c.size.atLeast(TreeSize.MEDIUM)) {
            int side = c.randInt(2, 3 + c.size.ordinal() / 2);
            for (int i = 0; i < side; i++) {
                Vec3 s = Shapes.pointAtHeight(trunk, c.base.y + trunkH * c.rand(0.4, 0.7));
                Vec3 dir = Shapes.horizontal(c.randAngle());
                double len = tr + c.rand(2, 4);
                Vec3 e = s.add(dir.scale(len)).add(0, len * 0.4, 0);
                Shapes.limb(c, s, e, 0.5, 0.5, Kind.LOG);
                Shapes.blob(c, e, cr * 0.6, cr * 0.45, cr * 0.6, 0.5, 0.3);
            }
        }
        Shapes.sideVines(c, 0.1, Math.max(3, (int) (H * 0.3)));
        Shapes.cocoa(c, 3 + c.size.ordinal() * 2, c.baseY() + 2, c.baseY() + (int) (trunkH * 0.6));
    }
}
