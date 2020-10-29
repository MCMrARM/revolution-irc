package io.mrarm.irc.build

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class SettingsBuilderPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def android = (AppExtension) project.extensions.getByName("android")

        def genDir = new File(project.getBuildDir(), "generated/source/settings")
        def genTask = project.task("generateSettings") {
            doLast {
                SettingsBuilder.generateJavaFiles(project.file("settings.yml"), genDir)
            }
        }

        android.applicationVariants.all { v ->
            v.registerJavaGeneratingTask(genTask, genDir)
        }
    }

}