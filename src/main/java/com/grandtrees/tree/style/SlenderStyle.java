package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeCanvas.Kind;
import com.grandtrees.tree.TreeContext;
import com.grandtrees.tree.TreeParams;
import com.grandtrees.tree.geom.Shapes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Birch: tall narrow trunk, short upward branches and a tall oval canopy. */
public final class SlenderStyle implements TreeStyle {
    public static final SlenderStyle INSTANCE = new SlenderStyle();

    @Override
    public void generate(TreeContext c) {
        TreeParams p = c.params;
        double tr = Math.max(0.5, p.trunkRadius() * 0.8);
        double H = p.height() * c.rand(1.0, 1.1);
        Vec3 bend = Shapes.horizontal(c.randAngle()).scale(c.rand(0.3, 1.2));
        List<Vec3> trunk = List.of(c.base, c.base.add(bend).add(0, H * 0.5, 0),
                c.base.add(bend.scale(0.5)).add(c.rand(-0.6, 0.6), H, c.rand(-0.6, 0.6)));
        Shapes.path(c, trunk, tr, 0.5, Kind.LOG);
        Shapes.rootFlare(c, tr, p.roots() / 2, 0.7, false);

        double cr = p.clusterRadius() * 0.9;
        double reach = Math.max(1.5, p.canopyRadius() * 0.6 - cr);
        double a0 = c.randAngle();
        for (int i = 0; i < p.branches(); i++) {
            Vec3 s = Shapes.pointAtHeight(trunk, c.base.y + H * c.rand(0.45, 0.85));
            Vec3 dir = Shapes.horizontal(a0 + i * 2.4);
            double len = reach * c.rand(0.6, 1.0);
            Vec3 e = s.add(dir.scale(len)).add(0, len * c.rand(0.6, 1.2), 0);
            Shapes.limb(c, s, e, 0.5, 0.5, Kind.LOG);
            Shapes.blob(c, e, cr * 0.8, cr, cr * 0.8, 1.0, 0.3);
        }
        int count = 3 + c.size.ordinal() * 2;
        for (int i = 0; i < count; i++) {
            double t = (double) i / Math.max(1, count - 1);
            double h = H * (0.42 + 0.53 * t);
            // Widest in the lower-middle of the crown, narrowing to the top.
            double spread = p.canopyRadius() * 0.4 * Math.sin(Math.PI * (0.25 + 0.75 * (1 - t)));
            Vec3 center = Shapes.pointAtHeight(trunk, c.base.y + h)
                    .add(Shapes.horizontal(c.randAngle()).scale(c.rand(spread * 0.4, spread)));
            Shapes.blob(c, center, cr * c.rand(0.9, 1.2), cr * c.rand(1.0, 1.3), cr * c.rand(0.9, 1.2), 1.0, 0.35);
        }
        Vec3 top = trunk.get(trunk.size() - 1);
        Shapes.blob(c, top, cr * 0.8, cr * 1.1, cr * 0.8, 1.0, 0.3);
    }
}
