package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeParams;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.geom.Shapes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Classic oak: kinked trunk, a few thick limbs and clumpy flat-bottomed leaf pads. */
public final class RoundBroadleafStyle implements TreeStyle {
    public static final RoundBroadleafStyle INSTANCE = new RoundBroadleafStyle();

    @Override
    public void generate(TreeContext c) {
        TreeParams p = c.params;
        double tr = p.trunkRadius();
        double trunkH = p.height() * c.rand(0.38, 0.5);
        int kinks = c.size == TreeSize.SMALL ? 2 : 1 + c.random.nextInt(2);
        List<Vec3> trunk = StyleUtil.kinkedTrunkPoints(c, trunkH, kinks, tr + 0.8);
        Shapes.path(c, trunk, tr, Math.max(0.5, tr * 0.65), Kind.LOG);
        Shapes.rootFlare(c, tr, p.roots(), 1.0, false);

        double cr = p.clusterRadius();
        double span = Math.max(1, p.height() - trunkH - cr * 0.6);
        List<Vec3> mids = new ArrayList<>();
        List<Vec3> ends = StyleUtil.crownBranches(c, trunk, trunkH, p.branches(), p.canopyRadius() - cr * 0.9,
                0.55, span, 0.1, 0.7, tr * 0.6, c.size.atLeast(TreeSize.BIG), mids);
        ends.add(StyleUtil.leader(c, trunk.get(trunk.size() - 1), span * c.rand(0.75, 0.9), tr * 0.5));

        // Separate, flat-bottomed clumps with gaps between them, so limbs show through.
        for (Vec3 e : ends) {
            Shapes.blob(c, e.add(0, cr * 0.2, 0), cr * c.rand(0.85, 1.1), cr * c.rand(0.5, 0.62),
                    cr * c.rand(0.85, 1.1), 0.45, 0.4);
        }
        if (c.size.atLeast(TreeSize.BIG)) {
            for (Vec3 m : mids) {
                if (c.random.nextFloat() < 0.6f) {
                    Shapes.blob(c, m.add(0, cr * 0.3, 0), cr * 0.65, cr * 0.45, cr * 0.65, 0.45, 0.4);
                }
            }
        }
    }
}
