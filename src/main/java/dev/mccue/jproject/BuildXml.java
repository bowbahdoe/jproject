package dev.mccue.jproject;

import dev.mccue.jproject.model.ApplicationModule;

/**
 * build.xml to funnel to Ant.
 */
public final class BuildXml {

    private BuildXml() {}

    public static String contents(ApplicationModule applicationModule) {
        // language=xml
        return """
        <project xmlns:ivy="antlib:org.apache.ivy.ant" xmlns:jacoco="antlib:org.jacoco.ant">
            <property name="src.dir"     value="%s"/>
            <property name="build.dir"   value="%s"/>
            <property name="lib.dir" value="%s"/>
            <property name="classes.dir" value="${build.dir}/classes"/>
            <property name="jar.dir"     value="${build.dir}/jar"/>
        
            <property name="main-class"  value="%s"/>
        
            <target name="clean">
                <delete dir="${build.dir}"/>
            </target>
            
            <target name="tree">
                <ivy:dependencytree />
            </target>
            
            <target name="ensure-deps">
                <ivy:retrieve pattern="${lib.dir}/[conf]/[artifact]-[revision].[ext]"
                              sync="true" />
                              
                <mkdir dir="${lib.dir}/default" />
                <mkdir dir="${lib.dir}/test" />
                <mkdir dir="${lib.dir}/compile" />
                <mkdir dir="${lib.dir}/runtime" />
                              
                <path id="default.modulepath">
                    <fileset dir="${lib.dir}/default" includes="*.jar"/>
                </path>
                
                <path id="test.modulepath">
                    <path refid="default.modulepath"/>
                    <fileset dir="${lib.dir}/test" includes="*.jar"/>
                </path>
                
                <path id="compile.modulepath">
                    <path refid="default.modulepath"/>
                    <fileset dir="${lib.dir}/compile" includes="*.jar"/>
                </path>
                
                <path id="runtime.modulepath">
                    <path refid="default.modulepath"/>
                    <fileset dir="${lib.dir}/runtime" includes="*.jar"/>
                    <fileset dir="${jar.dir}" includes="*.jar"/>
                </path>
            </target>
        
            <target name="compile" depends="ensure-deps">
                <mkdir dir="${classes.dir}"/>
                <javac srcdir="${src.dir}"
                       modulepathref="compile.modulepath"
                       debug="true"
                       destdir="${classes.dir}"
                       includeantruntime="false"
                       release="17"
                />
                <copy todir="${classes.dir}">
                    <fileset dir="${src.dir}" excludes="**/*.java"/>
                </copy>
            </target>
       
            <target name="jar" depends="compile">
                <mkdir dir="${jar.dir}"/>
                <exec executable="jar">
                    <arg value="--create"/>
                    <arg value="--file"/>
                    <arg value="${jar.dir}/application.jar"/>
                    <arg value="--main-class"/>
                    <arg value="${main-class}"/>
                    <arg value="-C"/>
                    <arg value="${classes.dir}"/>
                    <arg value="."/>
                </exec>
            </target>
            
            <target name="run" depends="clean,jar">
                <java modulepathref="runtime.modulepath"
                      module="dev.mccue.example"
                      fork="true"/>
            </target>
        </project>
        """.formatted(
                Conventions.SRC_DIR,
                Conventions.TARGET_DIR,
                Conventions.DEPENDENCIES_PATH,
                applicationModule.mainClass()
        );

    }
}
