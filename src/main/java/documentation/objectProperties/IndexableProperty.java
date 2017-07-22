package documentation.objectProperties;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptIndexSignature;
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Optional;

/**
 * Indexable type based on official TypeScript docs.
 *
 * <pre>
 * interface StringArray {
 *    [index: number]: string;
 * }
 *
 * let myArray: StringArray;
 * myArray = ["Bob", "Fred"];
 * </pre>
 *
 * Created by matt on 27-Jun-17.
 */
public class IndexableProperty extends TypeScriptObjectProperty {

    private TypeScriptIndexSignature indexSignature;

    public IndexableProperty(TypeScriptIndexSignature indexSignature) {
        this.indexSignature = indexSignature;
    }

    @Override
    public String getType() {
        return getPropertyType();
    }

    @Override
    public Optional<String> getDocumentationPropertyName() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getCodeGenPropertyName() {
        return Optional.empty();
    }

    @Override
    public String getPropertyType() {
        // Unfortunately if getText is used, the JSDocComment is included too and cannot be easily removed.
        // Instead, the full index type is rebuilt so no comment is included if one exists.

        String readOnly = "";
        JSAttributeList attributeList = PsiTreeUtil.findChildOfType(indexSignature, JSAttributeList.class);
        if (attributeList != null && attributeList.hasModifier(JSAttributeList.ModifierType.READONLY)) {
            readOnly = "readonly ";
        }

        String paramName = indexSignature.getParameterNameElement() == null ? "" :
                indexSignature.getParameterNameElement().getText();
        String paramType = indexSignature.getParameterType() == null ? "" : indexSignature.getParameterType().getText();
        String returnType = indexSignature.getType() == null ? "" : ": " + indexSignature.getType().getText();
        return readOnly + "[" + paramName + ": " + paramType + "]" + returnType;
    }

    @Override
    public boolean isOptional() {
        // Indexable properties cannot be optional.
        return false;
    }
}
