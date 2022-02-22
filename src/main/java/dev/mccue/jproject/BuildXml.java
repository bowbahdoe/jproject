package dev.mccue.jproject;

/**
 * build.xml to funnel to Ant.
 */
public final class BuildXml {

    public BuildXml() {}


    public String contents() {
        // language=xml
        return """
        <project xmlns:ivy="antlib:org.apache.ivy.ant">
            <property name="src.dir"     value="src"/>
            <property name="build.dir"   value="build"/>
            <property name="classes.dir" value="${build.dir}/classes"/>
            <property name="jar.dir"     value="${build.dir}/jar"/>
        
            <property name="main-class"  value="oata.HelloWorld"/>
         
            <!-- Initialize Dependency Management -->
            <property name="ivy.install.version" value="2.4.0" />
        
            <condition property="ivy.home" value="${env.IVY_HOME}">
                <isset property="env.IVY_HOME" />
            </condition>
            <property name="ivy.home" value="${user.home}/.ant" />
            <property name="ivy.jar.dir" value="${ivy.home}/lib" />
            <property name="ivy.jar.file" value="${ivy.jar.dir}/ivy.jar" />
        
            <target name="download-ivy" unless="offline">
                <mkdir dir="${ivy.jar.dir}" />
                <get src="https://repo1.maven.org/maven2/org/apache/ivy/ivy/${ivy.install.version}/ivy-${ivy.install.version}.jar"
                     dest="${ivy.jar.file}" usetimestamp="true" />
            </target>
        
            <target name="init-ivy" depends="download-ivy">
                <path id="ivy.lib.path">
                    <fileset dir="${ivy.jar.dir}" includes="*.jar"/>
                </path>
                <taskdef resource="org/apache/ivy/ant/antlib.xml"
                         uri="antlib:org.apache.ivy.ant"
                         classpathref="ivy.lib.path"/>
            </target>
        
            <target name="clean">
                <delete dir="${build.dir}"/>
            </target>
            
            <target name="re">
                <echo message='javac -g -h'/>
                <ivy:retrieve />
                <ivy:dependencytree />
            </target>
        
            <target name="compile">
                <ivy:retrieve />
                <mkdir dir="${classes.dir}"/>
                <javac modulesourcepath="."
                       destdir="${classes.dir}"
                       includeantruntime="false"
                       release="17"
                />
        
                <echo message="copy-non-java,${classes.dir},../dest/dir"></echo>
                <copy todir="../dest/dir">
                    <fileset dir="${classes.dir}" excludes="**/*.java"/>
                </copy>
            </target>
        
            <target name="jar" depends="compile">
                <mkdir dir="${jar.dir}"/>
                <jar destfile="${jar.dir}/${ant.project.name}.jar" basedir="${classes.dir}">
                    <manifest>
                        <attribute name="Main-Class" value="${main-class}"/>
                    </manifest>
                </jar>
            </target>
        
            <target name="run" depends="jar">
                <java jar="${jar.dir}/${ant.project.name}.jar" fork="true"/>
            </target>
        
            <target name="clean-build" depends="clean,jar"/>
        
            <target name="main" depends="clean,run"/>
        
        </project>
        """;
    }
}
