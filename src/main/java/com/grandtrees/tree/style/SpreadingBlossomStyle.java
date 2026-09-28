package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeParams;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.geom.Shapes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Cherry: short trunk, wide low-rising limbs, flattened blossom clouds and petals on the ground. */
public final class SpreadingBlossomStyle implements TreeStyle {
    public static final SpreadingBlossomStyle INSTANCE = new SpreadingBlossomStyle();

    @Override
    public void generate(TreeContext c) {
        TreeParams p = c.params;
        double tr = p.trunkRadius();
        double trunkH = p.height() * c.rand(0.35, 0.45);
        List<Vec3> trunk = StyleUtil.kinkedTrunkPoints(c, trunkH, 1, tr + 0.6);
        Shapes.path(c, trunk, tr, Math.max(0.5, tr * 0.7), Kind.LOG);
        Shapes.rootFlare(c, tr, p.roots(), 1.0, false);

        double cr = p.clusterRadius();
        double span = Math.max(1, (p.height() - trunkH) * 0.75);
        List<Vec3> ends = StyleUtil.crownBranches(c, trunk, trunkH, p.branches() + 1, p.canopyRadius() - cr * 1.2,
                0.6, span, 0.05, 0.55, tr * 0.6, c.size.atLeast(TreeSize.MEDIUM), null);
        ends.add(StyleUtil.leader(c, trunk.get(trunk.size() - 1), span * c.rand(0.55, 0.7), tr * 0.5));
        for (Vec3 e : ends) {
            Shapes.blob(c, e.add(0, cr * 0.2, 0), cr * c.rand(1.05, 1.3), cr * 0.6, cr * c.rand(1.05, 1.3), 0.5, 0.35);
        }
        Shapes.leafStrands(c, 0.05, 0.25, 2, 0, c.canvas.computeLeafDistances());
        Shapes.petals(c, (int) (p.canopyRadius() * p.canopyRadius() * 0.6), p.canopyRadius());
    }
}
