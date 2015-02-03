package krasa.translatorGenerator;

import java.util.Set;

import krasa.translatorGenerator.assembler.TranslatorDto;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.refactoring.typeCook.Util;
import com.intellij.util.containers.ConcurrentHashSet;

public class Context {
	public Set<TranslatorDto> scheduled = new ConcurrentHashSet<TranslatorDto>();
	public boolean replaceMethods = true;
	private Editor editor;

	public Context(Editor editor) {
		this.editor = editor;
	}

	public boolean shouldTranslate(String canonicalText) {
		return canonicalText.startsWith("com.t_motion") || canonicalText.startsWith("krasa");
	}

	public boolean shouldTranslate(PsiType typeParameter) {
		return shouldTranslate(typeParameter.getCanonicalText());
	}

	public void scheduleTranslator(PsiType from, PsiType to) {
		TranslatorDto translatorDto = new TranslatorDto(from, to);
		scheduled.add(translatorDto);
	}

	public void scheduleTranslator(PsiClass fromImpl, PsiClass toImpl) {
		TranslatorDto translatorDto = new TranslatorDto(Util.getType(fromImpl), Util.getType(toImpl));
		scheduled.add(translatorDto);

	}

	public boolean hasAnyScheduled() {
		for (TranslatorDto translatorDto : scheduled) {
			if (!translatorDto.processed) {
				return true;
			}
		}
		return false;
	}

	public void processedTranslator(PsiType from, PsiType to) {
		TranslatorDto e = new TranslatorDto(from, to);
		e.processed = true;
		scheduled.add(e);
	}

	public Editor getEditor() {
		return editor;
	}

}
