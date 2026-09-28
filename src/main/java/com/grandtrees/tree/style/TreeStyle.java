package com.grandtrees.tree.style;

import com.grandtrees.tree.TreeContext;

/** A procedural tree shape. Implementations only draw into {@link TreeContext#canvas}. */
public interface TreeStyle {
    void generate(TreeContext c);
}
