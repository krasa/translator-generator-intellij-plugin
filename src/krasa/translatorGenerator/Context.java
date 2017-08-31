package krasa.translatorGenerator;

import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.refactoring.typeCook.Util;
import com.intellij.util.containers.ConcurrentHashSet;

import krasa.translatorGenerator.assembler.TranslatorDto;

/**
 * @author Vojtech Krasa
 */
public class Context {

	private static final Logger LOG = Logger.getInstance(Context.class.getName());

	public Set<TranslatorDto> scheduled = new ConcurrentHashSet<TranslatorDto>();
	public boolean replaceMethods = true;
	private Project project;
	private Editor editor;

	public Context(Project project, Editor editor) {
		this.project = project;
		this.editor = editor;
	}

	public Project getProject() {
		return project;
	}

	public boolean shouldTranslate(PsiType getterType, PsiType setterType) {
		if (GlobalSettings.getInstance().isTranslationExcluded(getterType)) {
			return false;
		}
		if (!getterType.getCanonicalText().equals(setterType.getCanonicalText())) {
			return true;
		}
		return GlobalSettings.getInstance().shouldTranslate(getterType.getCanonicalText());
	}

	public void scheduleTranslator(PsiType from, PsiType to) {

		if (from instanceof PsiClassReferenceType) {
			from = refreshType((PsiClassReferenceType) from);
		}

		if (to instanceof PsiClassReferenceType) {
			to = refreshType((PsiClassReferenceType) to);
		}

		add(new TranslatorDto(from, to));
	}

	public void scheduleJaxBCollectionTranslator(PsiType from, PsiType to) {
		add(new TranslatorDto(from, to).jaxbCollection());
	}

	private void add(TranslatorDto translatorDto) {
		if (!scheduled.contains(translatorDto)) {
			LOG.info("scheduling " + translatorDto);
			scheduled.add(translatorDto);
		}
	}

	public void scheduleTranslator(PsiClass fromImpl, PsiClass toImpl) {
		TranslatorDto translatorDto = new TranslatorDto(Util.getType(fromImpl), Util.getType(toImpl));
		add(translatorDto);
	}

	public boolean hasAnyScheduled() {
		for (TranslatorDto translatorDto : scheduled) {
			if (!translatorDto.processed) {
				return true;
			}
		}
		return false;
	}

	public void markTranslatorMethodProcessed(PsiType from, PsiType to) {
		TranslatorDto e = new TranslatorDto(from, to);
		e.processed = true;
		scheduled.add(e);
	}

	public Editor getEditor() {
		return editor;
	}

	@NotNull
	public PsiClassType refreshType(PsiClassReferenceType fromGetterTypeParameter) {
		PsiClassReferenceType psiClassReferenceType = fromGetterTypeParameter;
		PsiClass resolve = psiClassReferenceType.resolve();
		return JavaPsiFacade.getInstance(getProject()).getElementFactory().createType(resolve);
	}
}
