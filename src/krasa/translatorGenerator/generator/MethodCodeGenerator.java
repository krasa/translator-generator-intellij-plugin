package krasa.translatorGenerator.generator;

import static com.siyeh.ig.psiutils.CollectionUtils.isCollectionClassOrInterface;
import static krasa.translatorGenerator.Utils.capitalize;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
					s = translatorBody(s, fromImpls[0], to, inputVariable);
				} else {
					// to=1, from=N
					s = instanceOfTranslator(s, fromImpls, to);
				}
			} else {
				s += "\n " + Utils.todo() + "\n";
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
					s = instanceOfTranslator(s, toImpls, fromImpls);
				} else {
					// to=M, from=N hardcore
					s += "\n " + Utils.todo() + "M2N\n";
				}

			}
		}

		s += "}";
		return s;
	}

	private String instanceOfTranslator(String s, PsiClass[] toImpls, PsiClass[] fromImpls) {
		for (PsiClass fromImpl : fromImpls) {
			PsiClass to = getMatching(toImpls, fromImpl);
			s = instanceOfTranslator(s, fromImpl, to);
		}
		return s;
	}

	private String instanceOfTranslator(String s, PsiClass[] fromImpls, PsiClass to) {
		for (int i1 = 0; i1 < fromImpls.length; i1++) {
			PsiClass fromImpl = fromImpls[i1];
			s = instanceOfTranslator(s, fromImpl, to);
		}
		return s;
	}

	private String instanceOfTranslator(String s, PsiClass fromImpl, PsiClass to) {
		String inputVariable = "input";
		if (fromImpl != from) {
			inputVariable = "input" + fromImpl.getName();
			s += "if(input instanceof " + fromImpl.getQualifiedName() + "){";
			s += fromImpl.getQualifiedName() + " " + inputVariable + " = (" + fromImpl.getQualifiedName() + ") input;";
			s = translatorBody(s, fromImpl, to, inputVariable);
			s += "}";
		} else {
			if (!PsiUtil.isAbstractClass(to)) {
				inputVariable = "input";
				s = translatorBody(s, fromImpl, to, inputVariable);
			} else {
				s += "throw new java.lang.IllegalArgumentException(\"unable to translate:\"+ input);";
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
		PsiElement[] toImpls = createImplementationsSearcher().searchImplementations(to1, context.getEditor(), true, false);
		toImpls = ArrayUtil.reverseArray(toImpls);
		PsiClass[] psiClasses = new PsiClass[toImpls.length];
		for (int i = 0; i < toImpls.length; i++) {
			psiClasses[i] = (PsiClass) toImpls[i];
		}
		return psiClasses;
	}

	private String translatorBody(String s, PsiClass from, PsiClass to, String inputVariable) {
		PsiField[] toFields = to.getAllFields();
		PsiField[] fromFields = from.getAllFields();
		s += to.getQualifiedName() + " " + "result = new " + to.getQualifiedName() + "();";
		s = translateFields(s, fromFields, toFields, inputVariable);
		s += "return result;";
		return s;
	}

	private String translateFields(String s, PsiField[] fromFields, PsiField[] toFields, String inputVariable) {
		int lastTodo = -1;
		Map<String, PsiField> fromFieldsMap = getStringPsiFieldMap(fromFields);
		AtomicBoolean containsTODO = new AtomicBoolean();

		for (int i = 0; i < toFields.length; i++) {
			PsiField toField = toFields[i];
			if (Utils.isStatic(toField)) {
				continue;
			}
			PsiMethod toSetter = Utils.setter(toField);
			PsiMethod toGetter = Utils.getter(toField);
			PsiField fromField = fromFieldsMap.get(toField.getName());
			PsiMethod fromGetter = Utils.getter(fromField);

			if (toSetter == null && toGetter != null && fromGetter != null) {
				// jaxb collection
				if (isCollectionClassOrInterface(toGetter.getReturnType())) {
					s += translateCollection(inputVariable, toField, toGetter, fromField, fromGetter);
				} else {
					s += Utils.newLineTodo(containsTODO);
					PsiType setterType = toField.getType();
					s += "\n//result.set" + capitalize(toField.getName()) + "(" + getter(inputVariable, fromGetter, setterType) + ")\n";
				}
			} else if (toSetter != null && fromGetter != null) {
				s += "result." + toSetter.getName() + "(" + getter(inputVariable, fromGetter, setterType(toSetter)) + ");";
			} else if (toSetter != null && fromField == null) {
				s += Utils.newLineTodo(containsTODO);
				s += "\n//result." + toSetter.getName() + "(" + inputVariable + ".get" + capitalize(toField.getName())
						+ "());\n";
			} else if (toSetter != null) { // fromField != null && fromGetter ==
				// null
				s += Utils.newLineTodo(containsTODO);
				s += "\n//result." + toSetter.getName() + "(" + inputVariable + ".get" + capitalize(toField.getName()) + "());\n";
			} else {
				if (lastTodo != i - 1) {
					s += "\n";
				}
				lastTodo = i;
				s += Utils.newLineTodo(containsTODO);
				s += "\n//result." + toField.getName() + "\n";
			}
		}
		return s.replace("\n\n", "\n");
	}

	private String translateCollection(String inputVariable, PsiField toField, PsiMethod toGetter, PsiField fromField, PsiMethod fromGetter) {
		int length = getGenericsLength(toField);

		if (toGetter.getReturnType().equals(fromGetter.getReturnType()) && length == 1) {
			return "result." + fromGetter.getName() + "().addAll(input." + fromGetter.getName() + "());";
		} else if (toGetter.getReturnType().equals(fromGetter.getReturnType()) && length == 2) {
			return "result." + fromGetter.getName() + "().putAll(input." + fromGetter.getName() + "());";
		}

		String methodName = CollectionTranslator.methodName(fromGetter.getReturnType(), toGetter.getReturnType());
		context.scheduleTranslator(fromGetter.getReturnType(), toGetter.getReturnType());
		return methodName + "(result." + fromGetter.getName() + "(), " + "input." + fromGetter.getName() + "());";
	}

	private int getGenericsLength(PsiField toField) {
		PsiClassReferenceType toGetterType = (PsiClassReferenceType) toField.getType();
		PsiType[] toGetterTypeParameters = toGetterType.getReference().getTypeParameters();
		return toGetterTypeParameters.length;
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
			protected PsiElement[] filterElements(PsiElement element, PsiElement[] targetElements) {
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

	private String getter(String inputVariable, PsiMethod fromGetter, PsiType setterType) {
		// todo
		PsiType getterType = fromGetter.getReturnType();

		if (getterType instanceof PsiPrimitiveType) {
			return inputVariable + "." + fromGetter.getName() + "()";
		} else {
			if (getterType instanceof PsiClassReferenceType) {
				PsiClassReferenceType getterRefType = (PsiClassReferenceType) getterType;
				PsiClassReferenceType setterRefType = (PsiClassReferenceType) setterType;
				if (context.shouldTranslate(getterRefType, setterRefType)) {
					String className = getterRefType.getClassName();
					context.scheduleTranslator(getterType, setterType);
					return "translate" + className + "(" + inputVariable + "." + fromGetter.getName() + "())";
				} else {
					return inputVariable + "." + fromGetter.getName() + "()";
				}
			} else if (getterType instanceof PsiArrayType) {
				PsiArrayType psiArrayType = (PsiArrayType) getterType;
				// todo check type
				PsiArrayType setterRefType = (PsiArrayType) setterType;
				if (context.shouldTranslate(psiArrayType.getComponentType(), setterRefType.getComponentType())) {
					String className = psiArrayType.getComponentType().getPresentableText() + "Array";
					context.scheduleTranslator(getterType, setterType);
					return "translate" + className + "(" + inputVariable + "." + fromGetter.getName() + "())";
				} else {
					return inputVariable + "." + fromGetter.getName() + "()";
				}
			} else {
				return "\n " + Utils.todo() + getterType + "\n";
			}
		}
	}

	@NotNull
	private PsiType setterType(PsiMethod setter) {
		return setter.getParameterList().getParameters()[0].getType();
	}

}
