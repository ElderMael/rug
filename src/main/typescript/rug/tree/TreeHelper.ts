/**
 * Helper functions for working with TreeNodes in simple
 * cases where we don't need a path expression.
 */

import { ParentAwareTreeNode, TextTreeNode, TreeNode } from "./PathExpression";

export function hasTag(n: TreeNode, t: string): boolean {
    return n.nodeTags().indexOf(t) > -1;
}

export function findPathFromAncestor(n: ParentAwareTreeNode, nodeTest: (TextTreeNode) => boolean): string {
    const parent = n.parent() as any; // Makes checking for parent function later easy
    if (parent == null) {
        // We couldn't resolve the path
        return null;
    } else if (nodeTest(parent)) {
        // console.log(`Gotcha: Parent is ${parent}`)
        // TODO what if it's not unique - need position, but then parent.children may reinitialize.
        // Not if a mutable container, admittedly
        return `/${n.nodeName()}`;
    } else if (parent.parent()) { // Essentially an instanceof, which we can't do on an interface
        return findPathFromAncestor(parent as ParentAwareTreeNode, nodeTest) + `/${n.nodeName()}`;
    } else {
        return null;
    }
}

export function findPathFromAncestorWithTag(n: ParentAwareTreeNode, tag: string): string {
    const r = findPathFromAncestor(
        n,
        (node) => node.nodeTags().contains(tag));
    return r;
}

/**
 * Return an ancestor meeting the given criteria
 * or null if it cannot be found
 */
export function findAncestor<N extends TreeNode>(n: ParentAwareTreeNode, nodeTest: (N) => boolean): N {
    const parent = n.parent() as any; // Makes checking for parent function later easy
    if (parent == null) {
        return null;
    } else if (nodeTest(parent)) {
        // console.log(`Gotcha: Parent is ${parent}`)
        return parent as N;
    } else if (parent.parent()) { // Essentially an instanceof, which we can't do on an interface
        return findAncestor(parent as ParentAwareTreeNode, nodeTest) as N;
    } else {
        return null;
    }
}

/**
 * Find an ancestor with a given tag
 */
export function findAncestorWithTag<N extends TreeNode>(n: ParentAwareTreeNode, tag: string): N {
    const r = findAncestor<N>(
        n,
        (node) => node.nodeTags().indexOf(tag) !== -1,
    );
    return r;
}

export type NodeStringifier = (TextTreeNode) => string;

export function nodeAndTagsStringifier(tn: TextTreeNode): string {
    if (tn.children().length === 0) {
        return `${tn.nodeName()}:[${tn.value()}]`;
    }
    return `${tn.nodeName()}:[${tn.nodeTags().join(", ")}]`;
}

export function nodeAndValueStringifier(tn: TextTreeNode, maxLen: number = 30): string {
    if (tn.children().length === 0 || tn.value().length < maxLen) {
        return `${tn.nodeName()}:[${tn.value()}]`;
    }
    return `${tn.nodeName()}:len=${tn.value().length}`;
}

function stringifyInternal(tn: TextTreeNode, nodeStringifier: NodeStringifier): string[] {
    const shorterString = nodeStringifier(tn);

    const lines = [shorterString].concat(
        // oh for flatMap...
        flatten(tn.children()
            .filter((k) => (k as TextTreeNode).value().length > 0)
            .map((k) => stringifyInternal(k as TextTreeNode, nodeStringifier))),
    );
    return lines.filter((l) => l.length > 0).map((l) => "  " + l);
}

// TODO move to a utility module
export const flatten = (arr) => arr.reduce(
    (acc, val) => acc.concat(
        Array.isArray(val) ? flatten(val) : val,
    ),
    [],
);

/**
 * Pretty string dump
 */
export function stringify(tn: TextTreeNode, nodeStringifier: NodeStringifier = nodeAndValueStringifier): string {
    return stringifyInternal(tn, nodeStringifier).join("\n");
}

/**
 * Use as replacer argument in JSON.stringify.
 * Essentially a curried functionality, allowing us to pass in a stringifier
 */
export function nodeReplacer(nodeStringifier: NodeStringifier = nodeAndValueStringifier) {
    return (key, value) => {
        if (value.nodeTags && value.value && value.children) {
            // TextTreeNode
            return {
                __kind__: "TextTreeNode",
                name: value.nodeName(),
                tags: value.nodeTags().join(","),
                structure: nodeStringifier(value),
            };
        } else if (value.nodeTags) {
            return {
                __kind__: "GraphNode",
                name: value.nodeName(),
                tags: value.nodeTags().join(","),
                toString: value + "",
            };
        }
        return value;
    };
}
