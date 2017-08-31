package krasa.translatorGenerator.generator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.generate.psi.PsiAdapter;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.Utils;

public class CollectionTranslator {
	private final PsiClassType from;
	private final PsiClassType to;
	private final boolean jaxbCollection;
	private final Context context;

	public CollectionTranslator(PsiClassType from, PsiClassType to, boolean jaxbCollection, Context context) {
		this.from = from;
		this.to = to;
		this.jaxbCollection = jaxbCollection;
		this.context = context;
	}

	public String translatorMethod() {
		String fromQualifiedName = from.resolve().getQualifiedName();
		String toQualifiedName = to.resolve().getQualifiedName();

		String s;
		if (jaxbCollection) {
			s = "public void " + methodName(from, to) + "(" + from.getPresentableText() + " input," + to.getCanonicalText() + " result) { ";
		} else {
			s = "public " + to.getCanonicalText() + " " + methodName(from, to) + "(" + from.getPresentableText() + " input) { ";
		}

		s += body();
		s += "}";
		return s;
	}

	@NotNull
	private String body() {
		String s = "";
		PsiClassType toType = to;
		PsiType[] toTypeParameters = PsiType.EMPTY_ARRAY;

		String toTypeStr;
		if (to instanceof PsiClassReferenceType) {
			toTypeParameters = ((PsiClassReferenceType) to).getReference().getTypeParameters();
			toTypeStr = toType.getCanonicalText();
		} else {
			toTypeParameters = to.getParameters();
			toTypeStr = toType.getCanonicalText();
		}

		PsiClassType fromType;
		PsiType[] fromTypeParameters = PsiType.EMPTY_ARRAY;
		fromType = from;
		if (from instanceof PsiClassReferenceType) {
			fromTypeParameters = ((PsiClassReferenceType) from).getReference().getTypeParameters();
		} else {
			fromTypeParameters = from.getParameters();
		}

		String inputVariable = "input";
		String objectType = "Object";
		String itemGetter = "item";
		String constructor = constructor(fromType, fromTypeParameters);
		if (jaxbCollection) {
			s += "if(input==null){return;}";

			s += "if(result==null){result=" + constructor + ";\n}\n";
		} else {
			s += toTypeStr + " result =" + constructor + ";\n";
		}

		if (toTypeParameters.length == 1 && fromTypeParameters.length == 1) { // List/Set


			PsiType toGetterTypeParameter = toTypeParameters[0];
			PsiType fromGetterTypeParameter = fromTypeParameters[0];

			objectType = fromGetterTypeParameter.getCanonicalText();
			boolean shouldTranslate = context.shouldTranslate(toGetterTypeParameter, fromGetterTypeParameter);
			if (toType.equals(fromType) && !shouldTranslate) {
				s += "result.addAll(input);";
				if (jaxbCollection) {
					return s;
				} else {
					return s + "return result;";
				}
			} else if (shouldTranslate) {
				context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter);
				itemGetter = "translate" + fromGetterTypeParameter.getPresentableText() + "(item)";
			}
		} else if (toTypeParameters.length == 2 && fromTypeParameters.length == 2) { // Map
			boolean shouldTranslate0 = context.shouldTranslate(toTypeParameters[0], fromTypeParameters[0]);
			boolean shouldTranslate1 = context.shouldTranslate(toTypeParameters[1], fromTypeParameters[1]);
			if (toType.equals(fromType) && !shouldTranslate0 && !shouldTranslate1) {
				s += "result.putAll(input);";
				if (jaxbCollection) {
					return s;
				} else {
					return s + "return result;";
				}
			}

			inputVariable += ".entrySet()";
			PsiType toGetterTypeParameter = toTypeParameters[0];
			PsiType fromGetterTypeParameter = fromTypeParameters[0];
			objectType = "java.util.Map.Entry<" + fromTypeParameters[0].getCanonicalText() + "," + fromTypeParameters[1].getCanonicalText() + ">";

			String key = "item.getKey()";
			String value = "item.getValue()";

			if (shouldTranslate0) {
				context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter);
				key = "translate" + fromGetterTypeParameter.getPresentableText() + "(item.getKey())";
			}
			if (shouldTranslate1) {
				context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter);
				value = "translate" + fromGetterTypeParameter.getPresentableText() + "(item.getValue())";
			}
			itemGetter = key + ", " + value;
		}

		s += "for(" + objectType + " item : " + inputVariable + "){";
		if (toTypeParameters.length == 2) {
			s += "result.put(" + itemGetter + ");";
		} else {
			s += "result.add(" + itemGetter + ");";
		}
		s += "}";

		if (!jaxbCollection) {
			s += "return result;";
		}
		return s;
	}

	@NotNull
	private String constructor(PsiClassType fromGetterType, PsiType[] fromGetterTypeParameters) {
		String impl = null;
		if (PsiAdapter.isListType(JavaPsiFacade.getElementFactory(context.getProject()), fromGetterType))
			impl = "java.util.ArrayList";
		else if (PsiAdapter.isSetType(JavaPsiFacade.getElementFactory(context.getProject()), fromGetterType)) {
			impl = "java.util.HashSet";
		} else if (PsiAdapter.isMapType(JavaPsiFacade.getElementFactory(context.getProject()), fromGetterType)) {
			impl = "java.util.HashMap";
		}

		String generics = (fromGetterTypeParameters.length > 0) ? "<>" : "";

		return "new " + impl + generics + "()";
	}

	@NotNull
	static String methodName(PsiType from, PsiType to) {
		return "translate" + Utils.getPresentableFullType(from) + "To" + Utils.getPresentableFullType(to);
	}

}
