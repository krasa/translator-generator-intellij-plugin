package krasa.translatorGenerator.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.Utils;

/**
 * @author Vojtech Krasa
 */
public class GetterCallsGenerator extends MethodCodeGenerator {

	private static final Logger LOG = Logger.getInstance("#" + MethodCodeGenerator.class.getName());

	private PsiLocalVariable identifier;
	private Context context;

	public GetterCallsGenerator(PsiLocalVariable identifier, Context context) {
		this.identifier = identifier;
		this.context = context;
	}

	public List<String> generateGetterCalls() {
		PsiClassReferenceType type = (PsiClassReferenceType) identifier.getType();
		PsiClass resolve = type.resolve();
		PsiField[] allFields = resolve.getAllFields();
		return generateCallsForFields(allFields, identifier.getName());
	}

	private List<String> generateCallsForFields(PsiField[] fields, String inputVariable) {
		List<String> strings = new ArrayList<String>();
		AtomicBoolean containsTODO = new AtomicBoolean();
		for (int i = 0; i < fields.length; i++) {
			String s = "";
			PsiField field = fields[i];
			if (Utils.isStatic(field)) {
				continue;
			}
			PsiMethod getter = Utils.getter(field);
			PsiType type = field.getType();

			if (getter != null) {
				s += type.getCanonicalText() + " " + field.getName() + "=" + inputVariable + "." + getter.getName() + "();";
			} else {
				// if (!containsTODO.getAndSet(true)) {
				// s += Utils.todo() + "\n";
				//
				// }

				s += type.getCanonicalText() + " " + field.getName() + "=" + inputVariable + "." + ";";

			}
			strings.add("\n" + s);
		}
		return strings;
	}
}
