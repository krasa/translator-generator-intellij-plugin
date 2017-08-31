package krasa.translatorGenerator.generator;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;

import krasa.translatorGenerator.Context;

public class ArrayTranslator {
	private final PsiClass from;
	private final PsiClass to;
	private final Context context;

	public ArrayTranslator(PsiClass from, PsiClass to, Context context) {

		this.from = from;
		this.to = to;
		this.context = context;
	}

	public String arrayTranslatorMethod() {
		String fromClassName = from.getName();
		String fromQualifiedName = from.getQualifiedName();
		String toQualifiedName = to.getQualifiedName();

		String s = "public " + toQualifiedName + "[] " + "translate" + fromClassName + "Array(" + fromQualifiedName + "[] input) { ";

		s += to.getQualifiedName() + "[] " + "result = new " + to.getQualifiedName() + "[input.length];";
		s += "for (int i = 0; i < input.length; i++) {";

		PsiClassType fromTypeParameter = JavaPsiFacade.getInstance(from.getProject()).getElementFactory().createType(from);
		PsiClassType toTypeParameter = JavaPsiFacade.getInstance(to.getProject()).getElementFactory().createType(to);
		if (context.shouldTranslate(toTypeParameter, fromTypeParameter)) {
			context.scheduleTranslator(fromTypeParameter, toTypeParameter);
			s += "result[i] = translate" + fromTypeParameter.getPresentableText() + "(input[i]);";
		} else {
			s += "result[i] = input[i];";
		}

		s += "}";

		s += "return result;}";
		return s;
	}
}
