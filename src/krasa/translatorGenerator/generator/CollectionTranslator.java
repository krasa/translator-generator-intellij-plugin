package krasa.translatorGenerator.generator;

import org.jetbrains.annotations.NotNull;

import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.Utils;

public class CollectionTranslator {
	private final PsiClassType from;
	private final PsiClassType to;
	private final Context context;

	public CollectionTranslator(PsiClassType from, PsiClassType to, Context context) {
		this.from = from;
		this.to = to;
		this.context = context;
	}

	public String translatorMethod() {
		String fromQualifiedName = from.resolve().getQualifiedName();
		String toQualifiedName = to.resolve().getQualifiedName();

		String s = "public void " + methodName(from, to) + "(" + from.getPresentableText() + " input," + to.getCanonicalText() + " result) { ";

		s += body();
		s += "}";
		return s;
	}

	@NotNull
	private String body() {
		String s = "";
		PsiClassReferenceType toGetterType = (PsiClassReferenceType) to;
		String toType = toGetterType.getCanonicalText();
		PsiType[] toGetterTypeParameters = toGetterType.getReference().getTypeParameters();

		PsiClassReferenceType fromGetterType = (PsiClassReferenceType) from;
		String fromType = fromGetterType.getCanonicalText();
		PsiType[] fromGetterTypeParameters = fromGetterType.getReference().getTypeParameters();

		String inputVariable = "input";
		String objectType = "Object";
		String itemGetter = "item";
		if (toGetterTypeParameters.length == 1 && fromGetterTypeParameters.length == 1) { // List/Set
			if (toGetterType.equals(fromGetterType)) {
				return "result.addAll(input);";
			}

			PsiType toGetterTypeParameter = toGetterTypeParameters[0];
			PsiType fromGetterTypeParameter = fromGetterTypeParameters[0];

			objectType = fromGetterTypeParameter.getCanonicalText();
			if (context.shouldTranslate(toGetterTypeParameter, fromGetterTypeParameter)) {
				context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter);
				itemGetter = "translate" + fromGetterTypeParameter.getPresentableText() + "(item)";
			}
		} else if (toGetterTypeParameters.length == 2 && fromGetterTypeParameters.length == 2) { // Map
			if (toGetterType.equals(fromGetterType)) {
				return "result.putAll(input);";
			}

			inputVariable += ".entrySet()";
			PsiType toGetterTypeParameter = toGetterTypeParameters[0];
			PsiType fromGetterTypeParameter = fromGetterTypeParameters[0];
			objectType = "java.util.Map.Entry<" + fromGetterTypeParameters[0].getCanonicalText() + "," + fromGetterTypeParameters[1].getCanonicalText() + ">";

			String key = "item.getKey()";
			String value = "item.getValue()";

			if (context.shouldTranslate(toGetterTypeParameters[0], fromGetterTypeParameters[0])) {
				context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter);
				key = "translate" + fromGetterTypeParameter.getPresentableText() + "(item.getKey())";
			}
			if (context.shouldTranslate(toGetterTypeParameters[1], fromGetterTypeParameters[1])) {
				context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter);
				value = "translate" + fromGetterTypeParameter.getPresentableText() + "(item.getValue())";
			}
			itemGetter = key + ", " + value;
		}
		s += "for(" + objectType + " item : " + inputVariable + "){";
		if (toGetterTypeParameters.length == 2) {
			s += "result.put(" + itemGetter + ");";
		} else {
			s += "result.add(" + itemGetter + ");";
		}
		s += "}";
		return s;
	}

	@NotNull
	static String methodName(PsiType from, PsiType to) {
		return "translate" + Utils.getPresentableFullType(from) + "To" + Utils.getPresentableFullType(to);
	}

}
