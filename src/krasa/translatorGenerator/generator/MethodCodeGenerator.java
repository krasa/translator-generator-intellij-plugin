package krasa.translatorGenerator.generator;

import static com.siyeh.ig.psiutils.CollectionUtils.isCollectionClassOrInterface;
import static krasa.translatorGenerator.Utils.capitalize;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.navigation.ImplementationSearcher;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ArrayUtil;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.Utils;

/**
 * @author Vojtech Krasa
 */
public class MethodCodeGenerator {

	private static final Logger LOG = Logger.getInstance("#" + MethodCodeGenerator.class.getName());

	private PsiClass from;
	private PsiClass to;
	private Context context;

	public MethodCodeGenerator() {
	}

	public MethodCodeGenerator(PsiClass from, PsiClass to, Context context) {
		this.from = from;
		this.to = to;
		this.context = context;
	}

	public String translatorMethod() {
		String fromClassName = from.getName();
		String fromQualifiedName = from.getQualifiedName();
		String toQualifiedName = to.getQualifiedName();

		String s = "public " + toQualifiedName + " " + "translate" + fromClassName + "(" + fromQualifiedName
				+ " input) { ";
		if (to.isEnum()) {
			s += "return " + toQualifiedName + ".valueOf(input);";
			s += "}";
			return s;
		}
		s += "if(input==null){return null;}";

		PsiClass[] toImpls = getImplementingClasses(to);
		PsiClass[] fromImpls = getImplementingClasses(from);
		String inputVariable = "input";
		if (toImpls.length == 1) {
			if (!PsiUtil.isAbstractClass(to)) {
				if (fromImpls.length == 1) {
					// to=1, from=1
					s = translator(s, fromImpls[0], to, inputVariable);
				} else {
					// to=1, from=N
					s = translator(s, fromImpls, to);
				}
			} else {
				s += "\n //TODO\n";
			}
		} else {
			if (fromImpls.length == 1) {
				// to=N, from=1
				PsiClass fromImpl = fromImpls[0];
				for (int i = 0; i < toImpls.length; i++) {
					PsiClass toImpl = toImpls[i];
					if (i > 0 && toImpls.length > 2) {
						s += "else if(TODO){\n";
						s += "return translate" + capitalize(fromImpl.getName()) + "To" + toImpl.getName()
								+ "(input);}";
					} else if (i > 0 && toImpls.length == 2) {
						s += "else {\n";
						s += "return translate" + capitalize(fromImpl.getName()) + "To" + toImpl.getName()
								+ "(input);}";
					} else {
						s += "if {\n";
						s += "return translate" + capitalize(fromImpl.getName()) + "To" + toImpl.getName()
								+ "(input);}";
					}
					context.scheduleTranslator(fromImpl, toImpl);
					s += "}\n";
				}
			} else {
				if (canTranslate(fromImpls, toImpls)) {
					// to=N, from=N
					s = translator(s, toImpls, fromImpls);
				} else {
					// to=M, from=N hardcore
					s += "\n //TODO M2N\n";
				}

			}
		}

		s += "}";
		return s;
	}

	public String arrayTranslatorMethod() {
		String fromClassName = from.getName();
		String fromQualifiedName = from.getQualifiedName();
		String toQualifiedName = to.getQualifiedName();

		String s = "public " + toQualifiedName + "[] " + "translate" + fromClassName + "Array(" + fromQualifiedName
				+ "[] input) { ";

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

	private String translator(String s, PsiClass[] toImpls, PsiClass[] fromImpls) {
		String inputVariable;
		for (PsiClass fromImpl : fromImpls) {
			PsiClass toImpl = getMatching(toImpls, fromImpl);
			if (fromImpl != from) {
				inputVariable = "input" + fromImpl.getName();
				s += "if(input instanceof " + fromImpl.getQualifiedName() + "){";
				s += fromImpl.getQualifiedName() + " " + inputVariable + " = (" + fromImpl.getQualifiedName()
						+ ") input;";
				s = translator(s, fromImpl, toImpl, inputVariable);
				s += "}";
			} else {
				if (!PsiUtil.isAbstractClass(toImpl)) {
					inputVariable = "input";
					s = translator(s, fromImpl, toImpl, inputVariable);
				} else {
					s += "throw new java.lang.IllegalArgumentException(\"unable to translate:\"+ input);";
				}
			}
		}
		return s;
	}

	private String translator(String s, PsiClass[] fromImpls, PsiClass to) {
		String inputVariable = "input";
		for (int i1 = 0; i1 < fromImpls.length; i1++) {
			PsiClass fromImpl = fromImpls[i1];
			if (fromImpl != from) {
				inputVariable = "input" + fromImpl.getName();
				s += "if(input instanceof " + fromImpl.getQualifiedName() + "){";
				s += fromImpl.getQualifiedName() + " " + inputVariable + " = (" + fromImpl.getQualifiedName()
						+ ") input;";
				s = translator(s, fromImpl, to, inputVariable);
				s += "}";
			} else {
				if (!PsiUtil.isAbstractClass(to)) {
					inputVariable = "input";
					s = translator(s, fromImpl, to, inputVariable);
				} else {
					s += "throw new java.lang.IllegalArgumentException(\"unable to translate:\"+ input);";
				}
			}
		}
		return s;
	}

	@NotNull
	private PsiClass getMatching(PsiClass[] toImpls, PsiClass fromImpl) {
		for (PsiClass toImpl : toImpls) {
			if (toImpl.getName().equals(fromImpl.getName())) {
				return fromImpl;
			}
		}
		return null;
	}

	private boolean canTranslate(PsiClass[] fromImpls, PsiClass[] toImpls) {
		if (fromImpls.length != toImpls.length) {
			return false;
		}
		Set<String> fromFieldsMap = names(fromImpls);
		Set<String> toStringPsiFieldMap = names(toImpls);

		return fromFieldsMap.equals(toStringPsiFieldMap);
	}

	private Set<String> names(PsiClass[] toImpls) {
		HashSet<String> strings = new HashSet<String>();
		for (PsiClass toImpl : toImpls) {
			strings.add(toImpl.getName());
		}
		return strings;
	}

	private PsiClass[] getImplementingClasses(PsiClass to1) {
		PsiElement[] toImpls = createImplementationsSearcher().searchImplementations(to1, context.getEditor(), 0, true,
				false);
		toImpls = ArrayUtil.reverseArray(toImpls);
		PsiClass[] psiClasses = new PsiClass[toImpls.length];
		for (int i = 0; i < toImpls.length; i++) {
			psiClasses[i] = (PsiClass) toImpls[i];
		}
		return psiClasses;
	}

	private String translator(String s, PsiClass from, PsiClass to, String inputVariable) {
		PsiField[] toFields = to.getAllFields();
		PsiField[] fromFields = from.getAllFields();
		s += to.getQualifiedName() + " " + "result = new " + to.getQualifiedName() + "();";
		s = generateCall(s, fromFields, toFields, inputVariable);
		s += "return result;";
		return s;
	}

	private String generateCall(String s, PsiField[] fromFields, PsiField[] toFields, String inputVariable) {
		int lastTodo = -1;
		Map<String, PsiField> fromFieldsMap = getStringPsiFieldMap(fromFields);

		for (int i = 0; i < toFields.length; i++) {
			PsiField toField = toFields[i];
			if (Utils.isStatic(toField)) {
				continue;
			}
			PsiMethod setter = Utils.setter(toField);
			PsiMethod toGetter = Utils.getter(toField);

			if (setter == null && toGetter != null) {
				if (isCollectionClassOrInterface(toGetter.getReturnType())) {
					s = handleCollection(s, toField, toGetter, inputVariable, fromFieldsMap);
				} else {
					s += " //TODO " + toField.getName() + "\n";
				}
			} else if (setter != null) {
				PsiField fromField = fromFieldsMap.get(toField.getName());
				if (fromField == null) {
					s += "\n//result." + setter.getName() + "(" + inputVariable + ".get" + capitalize(toField.getName())
							+ "());\n";
				} else {
					PsiMethod fromGetter = Utils.getter(fromField);
					if (fromGetter != null) {
						s += "result." + setter.getName() + "(" + getter(fromGetter, setter, inputVariable) + ");";
					} else {
						s += "\n//result." + setter.getName() + "(" + inputVariable + ".get"
								+ capitalize(toField.getName()) + "());\n";
					}
				}
			} else {
				if (lastTodo != i - 1) {
					s += "\n";
				}
				lastTodo = i;
				s += " //TODO " + toField.getName() + "\n";
			}
		}
		return s.replace("\n\n", "\n");
	}

	protected Map<String, PsiField> getStringPsiFieldMap(PsiField[] fromFields) {
		Map<String, PsiField> fromFieldsMap = new HashMap<String, PsiField>();
		for (PsiField fromField : fromFields) {
			fromFieldsMap.put(fromField.getName(), fromField);
		}
		return fromFieldsMap;
	}

	protected static ImplementationSearcher createImplementationsSearcher() {
		return new ImplementationSearcher() {

			@Override
			protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements, int offset) {
				return MethodCodeGenerator.filterElements(targetElements);
			}
		};
	}

	private static PsiElement[] filterElements(final PsiElement[] targetElements) {
		final Set<PsiElement> unique = new LinkedHashSet<PsiElement>(Arrays.asList(targetElements));
		for (final PsiElement elt : targetElements) {
			ApplicationManager.getApplication().runReadAction(new Runnable() {

				@Override
				public void run() {
					final PsiFile containingFile = elt.getContainingFile();
					LOG.assertTrue(containingFile != null, elt);
					PsiFile psiFile = containingFile.getOriginalFile();
					if (psiFile.getVirtualFile() == null) {
						unique.remove(elt);
					}
				}
			});
		}
		// special case for Python (PY-237)
		// if the definition is the tree parent of the target element, filter
		// out the target element
		for (int i = 1; i < targetElements.length; i++) {
			final PsiElement targetElement = targetElements[i];
			if (ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {

				@Override
				public Boolean compute() {
					return PsiTreeUtil.isAncestor(targetElement, targetElements[0], true);
				}
			})) {
				unique.remove(targetElements[0]);
				break;
			}
		}
		return PsiUtilCore.toPsiElementArray(unique);
	}

	protected String handleCollection(String s, PsiField toField, PsiMethod toGetter, String methodInputVariable,
			Map<String, PsiField> fromFieldsMap) {
		if (toGetter.getReturnType() instanceof PsiClassReferenceType) {
			PsiClassReferenceType toGetterType = (PsiClassReferenceType) toGetter.getReturnType();
			String toType = toGetterType.getCanonicalText();
			PsiType[] toGetterTypeParameters = toGetterType.getReference().getTypeParameters();

			PsiField fromField = fromFieldsMap.get(toField.getNameIdentifier().getText());
			PsiMethod fromGetter = Utils.getter(fromField);
			PsiClassReferenceType fromGetterType = (PsiClassReferenceType) fromGetter.getReturnType();
			String fromType = fromGetterType.getCanonicalText();
			PsiType[] fromGetterTypeParameters = fromGetterType.getReference().getTypeParameters();

			String inputVariable = "input" + capitalize(fromField.getName());
			s += fromType + " " + inputVariable + " =  " + methodInputVariable + "." + fromGetter.getName() + "();";
			String resultVariable = "result" + capitalize(toField.getName());
			s += toType + " " + resultVariable + " =  result." + toGetter.getName() + "();";

			String objectType = "Object";
			String itemGetter = "item";
			if (toGetterTypeParameters.length == 1 && fromGetterTypeParameters.length == 1) { // List/Set
				PsiType toGetterTypeParameter = toGetterTypeParameters[0];
				PsiType fromGetterTypeParameter = fromGetterTypeParameters[0];
				objectType = fromGetterTypeParameter.getCanonicalText();
				if (context.shouldTranslate(toGetterTypeParameter, fromGetterTypeParameter)) {
					context.scheduleTranslator(fromGetterTypeParameter, toGetterTypeParameter);
					itemGetter = "translate" + fromGetterTypeParameter.getPresentableText() + "(item)";
				}
			} else if (toGetterTypeParameters.length == 2 && fromGetterTypeParameters.length == 2) { // Map
				inputVariable += ".entrySet()";
				PsiType toGetterTypeParameter = toGetterTypeParameters[0];
				PsiType fromGetterTypeParameter = fromGetterTypeParameters[0];
				objectType = "java.util.Map.Entry<" + fromGetterTypeParameters[0].getCanonicalText() + ","
						+ fromGetterTypeParameters[1].getCanonicalText() + ">";

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
				s += resultVariable + ".put(" + itemGetter + ");";
			} else {
				s += resultVariable + ".add(" + itemGetter + ");";
			}
			s += "}";
		} else {
			s += "\n//todo " + toGetter.getReturnType() + "\n";
		}
		return s;
	}

	protected String getter(PsiMethod getter, PsiMethod setter, String inputVariable) {
		// todo
		PsiType getterType = getter.getReturnType();
		PsiParameter psiParameter = setter.getParameterList().getParameters()[0];
		PsiType setterType = psiParameter.getType();

		if (getterType instanceof PsiPrimitiveType) {
			return inputVariable + "." + getter.getName() + "()";
		} else {
			if (getterType instanceof PsiClassReferenceType) {
				PsiClassReferenceType getterRefType = (PsiClassReferenceType) getterType;
				PsiClassReferenceType setterRefType = (PsiClassReferenceType) setterType;
				if (context.shouldTranslate(getterRefType, setterRefType)) {
					String className = getterRefType.getClassName();
					context.scheduleTranslator(getterType, setterType);
					return "translate" + className + "(" + inputVariable + "." + getter.getName() + "())";
				} else {
					return inputVariable + "." + getter.getName() + "()";
				}
			} else if (getterType instanceof PsiArrayType) {
				PsiArrayType psiArrayType = (PsiArrayType) getterType;
				// todo check type
				PsiArrayType setterRefType = (PsiArrayType) setterType;
				if (context.shouldTranslate(psiArrayType.getComponentType(), setterRefType.getComponentType())) {
					String className = psiArrayType.getComponentType().getPresentableText() + "Array";
					context.scheduleTranslator(getterType, setterType);
					return "translate" + className + "(" + inputVariable + "." + getter.getName() + "())";
				} else {
					return inputVariable + "." + getter.getName() + "()";
				}
			} else {
				return "\n //todo " + getterType + "\n";
			}
		}
	}

}
