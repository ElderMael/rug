import {Project, File} from "@atomist/rug/model/Core";
import {Editor} from "@atomist/rug/operations/Decorators";
import {PathExpression, TextTreeNode, TypeProvider, PathExpressionEngine} from "@atomist/rug/tree/PathExpression";
import * as yaml from "@atomist/rug/ast/yaml/Types";
import {YamlPathExpressionEngine} from "@atomist/rug/ast/yaml/YamlPathExpressionEngine";

@Editor("Edits")
class AddToNestedSequence  {

    edit(project: Project) {
        let eng: PathExpressionEngine =
            new YamlPathExpressionEngine(project.context.pathExpressionEngine)

        let findNested = `/*[@name='x.yml']/YamlFile()/components/Amplifier/*[@name='future upgrades']`

        eng.with<yaml.YamlSequence>(project, findNested, yamlValue => {
            // console.log(`${this.description}: text value is \n[${yamlValue.value()}]`)
            yamlValue.addElement('NAP500')
            //  console.log(`${this.description}: updated text value is \n[${yamlValue.value()}]`)
        })
    }

}

export let editor = new AddToNestedSequence()
