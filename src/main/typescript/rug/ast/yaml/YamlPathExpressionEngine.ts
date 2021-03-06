import { DecoratingPathExpressionEngine } from "../DecoratingPathExpressionEngine";

import { TextTreeNode, TreeNode } from "../../tree/PathExpression";
import * as helper from "../../tree/TreeHelper";
import * as yaml from "./Types";

/**
 * PathExpressionEngine decorator that returns Yaml type mixins for value nodes.
 */
export class YamlPathExpressionEngine extends DecoratingPathExpressionEngine {

    protected decoratorFor(n: TreeNode): any {
        if ((n as any).value) { // It's a text node
            const ttn = n as TextTreeNode;

            // console.log(`Decorating [${n}] with tags [${n.nodeTags()}]`)
            if (helper.hasTag(n, "Scalar")) {
                switch (ttn.value().charAt(0)) {
                    case '"':
                        return new yaml.YamlQuotedValue(ttn);
                    case ">":
                        switch (ttn.value().charAt(1)) {
                            case "-":
                                return new yaml.YamlFoldedBlockWithStripChomping(ttn);
                            case "+":
                                return new yaml.YamlFoldedBlockWithKeepChomping(ttn);
                            default:
                                return new yaml.YamlFoldedBlockScalar(ttn);
                        }
                    case "|":
                        switch (ttn.value().charAt(1)) {
                            case "-":
                                return new yaml.YamlLiteralBlockWithStripChomping(ttn);
                            case "+":
                                return new yaml.YamlLiteralBlockWithKeepChomping(ttn);
                            default:
                                return new yaml.YamlLiteralBlockScalar(ttn);
                        }
                    default:
                        return new yaml.YamlRawValue(ttn);
                }
            } else if (helper.hasTag(n, "Sequence")) {
                return new yaml.YamlSequenceOps(ttn);
            }
        }
        // If we didn't match anything
        return null;
    }
}
