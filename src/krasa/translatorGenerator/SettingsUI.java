package krasa.translatorGenerator;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;

import javax.swing.*;
import javax.swing.border.LineBorder;

import org.jetbrains.annotations.Nullable;

import com.intellij.ide.hierarchy.JavaHierarchyUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.ui.SortedListModel;
import com.siyeh.ig.psiutils.CollectionUtils;

public class SettingsUI {
	private JPanel root;
	private JList included;
	private JTextField type2;
	private JButton addType2;
	private JTextField type1;
	private JTextField excludeType;
	private JButton addType1;
	private JButton exclude;
	private JButton deleteIncluded;
	private JButton deleteExcluded;
	private JList excluded;

	public SettingsUI(PsiType fromType, PsiType toType) {
		excluded.setBorder(new LineBorder(Color.BLACK));
		included.setBorder(new LineBorder(Color.BLACK));
		type1.setText(getPackage(fromType));
		if (!TypeConversionUtil.isVoidType(toType)) {
			type2.setText(getPackage(toType));
		}
		initListModel();

		addType1.addActionListener(e -> {
			GlobalSettings.getInstance().included.add(type1.getText());
			initListModel();
		});
		addType2.addActionListener(e -> {
			GlobalSettings.getInstance().included.add(type2.getText());
			initListModel();
		});
		exclude.addActionListener(e -> {
			GlobalSettings.getInstance().excluded.add(excludeType.getText());
			initListModel();
		});
		deleteIncluded.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GlobalSettings.getInstance().included.remove(included.getSelectedValue());
				initListModel();
			}
		});
		deleteExcluded.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GlobalSettings.getInstance().excluded.remove(excluded.getSelectedValue());
				initListModel();
			}
		});
	}

	public static boolean showDialog(Project eventProject, PsiClass psi) {
		PsiType from = PsiTypesUtil.getClassType(psi);
		final SettingsUI settingsUI = new SettingsUI(from, from);
		return show(eventProject, settingsUI);
	}

	public static boolean showDialog(Project eventProject, PsiMethod psiMethod) {
		PsiParameter[] parameters = psiMethod.getParameterList().getParameters();
		PsiType fromType = parameters[0].getType();
		PsiType toType = psiMethod.getReturnType();
		if (TypeConversionUtil.isVoidType(toType) && parameters.length == 2) {
			toType = parameters[1].getType();
		}
		final SettingsUI settingsUI = new SettingsUI(fromType, toType);
		return show(eventProject, settingsUI);
	}

	private static boolean show(Project eventProject, SettingsUI settingsUI) {
		return new DialogWrapper(eventProject) {
			{
				init();
				setTitle("Translator Settings");
			}

			@Nullable
			@Override
			protected String getDimensionServiceKey() {
				return "translator-generator-intellij-plugin";
			}

			@Nullable
			@Override
			protected JComponent createCenterPanel() {
				return settingsUI.getRoot();
			}
		}.showAndGet();
	}

	private String getPackage(PsiType fromType) {
		String packageName = "";
		if (CollectionUtils.isCollectionClassOrInterface(fromType) && fromType instanceof PsiClassReferenceType) {
			PsiType[] toGetterTypeParameters = ((PsiClassReferenceType) fromType).getReference().getTypeParameters();
			if (toGetterTypeParameters.length > 0) {
				fromType = toGetterTypeParameters[0];
			}
		}
		if (fromType instanceof PsiClassType) {
			PsiClassType psiClassReferenceType = (PsiClassType) fromType;
			PsiClass resolve = psiClassReferenceType.resolve();
			packageName = JavaHierarchyUtil.getPackageName(resolve);
		}
		return packageName;
	}

	private void initListModel() {
		GlobalSettings instance = GlobalSettings.getInstance();
		SortedListModel<String> model = new SortedListModel<>(Comparator.<String> naturalOrder());
		model.addAll(instance.included);
		included.setModel(model);
		SortedListModel exModel = new SortedListModel(Comparator.naturalOrder());
		exModel.addAll(instance.excluded);
		excluded.setModel(exModel);
	}

	public JPanel getRoot() {
		return root;
	}
}
