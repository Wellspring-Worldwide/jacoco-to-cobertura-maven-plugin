package com.tomassatka;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.stream.Format;

@Mojo(name = "jacocoToCobertura", defaultPhase = LifecyclePhase.TEST)
public class JacocoToCoberturaMojo extends AbstractMojo {

    @Parameter(property = "inputFile", required = true)
    private File inputFile;

    @Parameter(property = "outputFile", required = true)
    private File outputFile;

    @Parameter(property = "sourceDirectories", required = true)
    private List<String> sourceDirectories;

    @Parameter(property = "splitByPackage", defaultValue = "false")
    private boolean splitByPackage;


    //TODO: temporary
    public void setInputFile(File inputFile) {
        this.inputFile = inputFile;
    }
    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }
    public void setSourceDirectories(List<String> sourceDirectories) {
        this.sourceDirectories = sourceDirectories;
    }
    public void setSplitByPackage(boolean splitByPackage) {
        this.splitByPackage = splitByPackage;
    }


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            new JacocoToCoberturaTask(inputFile, outputFile, sourceDirectories, splitByPackage, getLog()).execute();
        } catch (JacocoToCoberturaException | IOException e) {
            getLog().error("An error occurred during execution:", e);
        }
    }
}

class JacocoToCoberturaTask {

    private File inputFile;
    private File outputFile;
    private List<String> sourceDirectories;
    private boolean splitByPackage;
    private org.apache.maven.plugin.logging.Log log;

    public JacocoToCoberturaTask(File inputFile, File outputFile, List<String> sourceDirectories, boolean splitByPackage, org.apache.maven.plugin.logging.Log log) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        this.sourceDirectories = sourceDirectories;
        this.splitByPackage = splitByPackage;
        this.log = log;
    }

    public void execute() throws MojoExecutionException, JacocoToCoberturaException, IOException {
        log.info("Converting JaCoCo report to Cobertura");

        if (!inputFile.exists()) {
            throw new MojoExecutionException("File " + inputFile.getAbsolutePath() + " does not exist");
        }

        File outputDirectory = outputFile.getParentFile();
        if (!outputDirectory.exists()) {
            try {
                if (!outputDirectory.mkdirs()) {
                    throw new MojoExecutionException("`mkdirs()` returned false");
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Output file directory " + outputDirectory.getAbsolutePath()
                        + " does not exist and couldn't be created, error: " + e.getMessage());
            }
        }

        log.info("Calculated configuration:");
        log.info("  input: " + inputFile);
        log.info("  output: " + outputFile);
        log.info("  splitByPackage: " + splitByPackage);
        log.info("sourceDirs:");
        sourceDirectories.forEach(sourceDir -> log.info(" - " + sourceDir));

        Jacoco.Report jacocoData = loadJacocoData(inputFile);

        if (splitByPackage) {
            for (Jacoco.PackageElement packageElement : jacocoData.getPackages()) {
                String packageName = packageElement.getName().replace('/', '.');
                Jacoco.Report packageData = jacocoData.copyReportWithPackage(jacocoData, packageElement);
                File packageOut = new File(outputFile.getAbsolutePath().replace(".xml", "-" + packageName + ".xml"));
                writeCoberturaData(packageOut, transformData(packageData, sourceDirectories));
                log.info("Cobertura report for package " + packageName + " generated at "
                        + packageOut);
            }
        } else {
            writeCoberturaData(outputFile, transformData(jacocoData, sourceDirectories));
            log.info("Cobertura report generated at " + outputFile);
        }
    }

    private Jacoco.Report loadJacocoData(File inputFile) throws JacocoToCoberturaException {
        try {
            Serializer serializer = new Persister();
            return serializer.read(Jacoco.Report.class, inputFile);
        } catch (Exception e) {
            throw new JacocoToCoberturaException("Loading Jacoco report error: `" + e.getMessage() + "`");
        }
    }

    private Cobertura.Coverage transformData(Jacoco.Report jacocoData, Collection<String> sources)
            throws JacocoToCoberturaException {
        try {
            return new Cobertura.Coverage(jacocoData, sources);
        } catch (Exception e) {
            throw new JacocoToCoberturaException(
                    "Transforming Jacoco Data to Cobertura error: `" + e.getMessage() + "`");
        }
    }

    private void writeCoberturaData(File outputFile, Cobertura.Coverage data)
            throws JacocoToCoberturaException, IOException {
        try {
            Persister persister = new Persister(new Format("<?xml version=\"1.0\" encoding= \"UTF-8\" ?>"));
            persister.write(data, outputFile);
        } catch (Exception e) {
            throw new JacocoToCoberturaException("Writing Cobertura Data to file `" + outputFile.getCanonicalPath()
                    + "` error: `" + e.getMessage() + "`");
        }
    }

}