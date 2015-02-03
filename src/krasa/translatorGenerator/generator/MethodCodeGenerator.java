package krasa.translatorGenerator.generator;

import java.util.*;

import krasa.translatorGenerator.Context;
import krasa.translatorGenerator.Utils;

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

public class MethodCodeGenerator {
	private static final Logger LOG = Logger.getInstance("#" + MethodCodeGenerator.class.getName());

	private PsiClass from;
	private PsiClass to;
	private Context context;

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
						s += "return translate" + Utils.capitalize(fromImpl.getName()) + "To" + toImpl.getName()
								+ "(input);}";
					} else if (i > 0 && toImpls.length == 2) {
						s += "else {\n";
						s += "return translate" + Utils.capitalize(fromImpl.getName()) + "To" + toImpl.getName()
								+ "(input);}";
					} else {
						s += "if {\n";
						s += "return translate" + Utils.capitalize(fromImpl.getName()) + "To" + toImpl.getName()
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
		s = generateCallsForFields(s, fromFields, toFields, inputVariable);
		s += "return result;";
		return s;
	}

	private String generateCallsForFields(String s, PsiField[] fromFields, PsiField[] toFields, String inputVariable) {
		int lastTodo = -1;
		Map<String, PsiField> fromFieldsMap = getStringPsiFieldMap(fromFields);

		for (int i = 0; i < toFields.length; i++) {
			PsiField toField = toFields[i];
			if (Utils.isStatic(toField)) {
				continue;
			}
			PsiMethod setter = Utils.setter(toField);
			PsiMethod getter = Utils.getter(toField);

			if (setter == null && getter != null) {
				s = handleJaxbCollection(s, toField, getter, inputVariable);
			} else if (setter != null) {
				PsiField fromField = fromFieldsMap.get(toField.getName());
				if (fromField == null) {
					s += "\n//result." + setter.getName() + "(" + inputVariable + ".get"
							+ Utils.capitalize(toField.getName()) + "());\n";
				} else {
					PsiMethod fromGetter = Utils.getter(fromField);
					if (fromGetter != null) {
						s += "result." + setter.getName() + "(" + translatableGetter(fromGetter, setter, inputVariable)
								+ ");";
					} else {
						s += "\n//result." + setter.getName() + "(" + inputVariable + ".get"
								+ Utils.capitalize(toField.getName()) + "());\n";
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

	private Map<String, PsiField> getStringPsiFieldMap(PsiField[] fromFields) {
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
		// if the definition is the tree parent of the target element, filter out the target element
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

	private String handleJaxbCollection(String s, PsiField field, PsiMethod getter, String inputVariable) {
		if (getter.getReturnType() instanceof PsiClassReferenceType) {
			PsiClassReferenceType returnType = (PsiClassReferenceType) getter.getReturnType();
			String type = returnType.getCanonicalText();
			String capitalizedFieldName = Utils.capitalize(field.getName());

			s += type + " input" + capitalizedFieldName + " =  " + inputVariable + "." + getter.getName() + "();";
			s += type + " result" + capitalizedFieldName + " =  result." + getter.getName() + "();";
			PsiType[] typeParameters = returnType.getReference().getTypeParameters();
			String objectType = "Object";
			String itemGetter = "item";
			if (typeParameters.length > 0) {
				PsiType typeParameter = typeParameters[0];
				objectType = typeParameter.getCanonicalText();
				if (context.shouldTranslate(typeParameter)) {
					context.scheduleTranslator(typeParameter, typeParameter);
					itemGetter = "translate" + typeParameter.getPresentableText() + "(item)";
				}
				// todo handle Map
			}
			s += "for(" + objectType + " item : input" + capitalizedFieldName + "){";
			s += "	result" + capitalizedFieldName + ".add(" + itemGetter + ");";
			s += "}";
		} else {
			s += "\n//todo " + getter.getReturnType() + "\n";
		}
		return s;
	}

	private String translatableGetter(PsiMethod getter, PsiMethod setter, String inputVariable) {
		// todo
		PsiType getterType = getter.getReturnType();
		PsiParameter psiParameter = setter.getParameterList().getParameters()[0];
		PsiType setterType = psiParameter.getType();

		if (getterType instanceof PsiPrimitiveType) {
			return inputVariable + "." + getter.getName() + "()";
		} else {
			if (getterType instanceof PsiClassReferenceType) {
				PsiClassReferenceType psiTypeElement = (PsiClassReferenceType) getterType;
				if (context.shouldTranslate(psiTypeElement)) {
					String className = psiTypeElement.getClassName();
					context.scheduleTranslator(getterType, setterType);
					return "translate" + className + "(" + inputVariable + "." + getter.getName() + "())";
				} else {
					return inputVariable + "." + getter.getName() + "()";
				}
			} else if (getterType instanceof PsiArrayType) {
				PsiArrayType psiArrayType = (PsiArrayType) getterType;
				if (context.shouldTranslate(psiArrayType.getComponentType())) {
					String className = psiArrayType.getComponentType().getCanonicalText() + "Array";
					// todo array translator
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
