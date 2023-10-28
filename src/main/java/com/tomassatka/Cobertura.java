package com.tomassatka;

import java.util.Collection;
import java.util.Collections;

import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

public class Cobertura {
    public static List<Jacoco.Line> linesForMethod(Jacoco.MethodElement jMethod, Jacoco.PackageElement jPack,
            String jSource) {
        if (jSource == null) {
            return Collections.emptyList();
        } else {
            int currentMethodLine = (jMethod.getLine() != null) ? jMethod.getLine() : 0;
            List<Jacoco.Line> sourceLines = jPack.getSourceFiles().stream()
                    .filter(sf -> jSource.equals(sf.getName()))
                    .flatMap(sf -> sf.getLines().stream())
                    .filter(line -> (line.getNr() != 0 && line.getNr() >= currentMethodLine))
                    .collect(Collectors.toList());

            Map<String, Integer> packMethods = jPack.getClasses().stream()
                    .filter(ce -> jSource.equals(ce.getSourcefilename()))
                    .flatMap(ce -> ce.getMethods().stream())
                    .filter(method -> method.getName() != null && method.getLine() != null)
                    .collect(Collectors.toMap(
                            method -> (String) method.getName(),
                            method -> (Integer) method.getLine(),
                            (existingValue, newValue) -> newValue));

            int nextMethodLine = packMethods.values().stream()
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElse(Integer.MAX_VALUE);

            return sourceLines.stream()
                    .filter(line -> (line.getNr() != 0 && line.getNr() < nextMethodLine))
                    .collect(Collectors.toList());
        }
    }

    @Root(strict = false, name = "class")
    static class ClassElement {
        @Attribute(name = "name", required = true)
        private String name;

        @Attribute(name = "filename", required = true)
        private String filename;

        @Path("methods")
        @ElementList(name = "methods", required = false, inline = true)
        private List<Method> methods;

        @Attribute(name = "line-rate", required = false)
        protected double lineRate = 0.0;

        @Attribute(name = "branch-rate", required = false)
        protected double branchRate = 0.0;

        @Attribute(name = "complexity", required = false)
        protected double complexity = 0.0;

        public ClassElement(Jacoco.ClassElement c, Jacoco.PackageElement jPack) {
            this.name = c.getName() != null ? c.getName() : "";
            String packageName = (jPack.getName() != null) ? jPack.getName() : "";
            String sourceFilename = (c.getSourcefilename() != null) ? c.getSourcefilename() : "";
            this.filename = packageName + "/" + sourceFilename;
            this.methods = c.getMethods().stream()
                    .map(it -> new Method(it, c.getSourcefilename(), jPack))
                    .collect(Collectors.toList());

            this.lineRate = c.lineRate();
            this.branchRate = c.branchRate();
            this.complexity = c.complexity();
        }

        public double getLineRate() {
            return lineRate;
        }

        public double getBranchRate() {
            return branchRate;
        }

        public double getComplexity() {
            return complexity;
        }
    }

    @Root(strict = false, name = "condition")
    static class Condition {
        @Attribute(name = "number", required = false)
        private final int number = 0;

        @Attribute(name = "type", required = false)
        private final String type = "jump";

        @Attribute(name = "coverage", required = false)
        private String coverage;

        public Condition(String s) {
            this.coverage = s;
        }
    }

    @Root(strict = false, name = "coverage")
    static class Coverage {
        @Attribute(name = "timestamp", required = true)
        private final long timestamp;

        @Path("sources")
        @ElementList(name = "sources", required = false, inline = true)
        private final List<Source> sources;

        @Path("packages")
        @ElementList(name = "packages", required = false, inline = true)
        private final List<Package> packages;

        @Attribute(name = "line-rate", required = false)
        protected double lineRate = 0.0;

        @Attribute(name = "branch-rate", required = false)
        protected double branchRate = 0.0;

        @Attribute(name = "complexity", required = false)
        protected double complexity = 0.0;

        public Coverage(Jacoco.Report j, Collection<String> sources) {
            this.timestamp = j.timestamp();
            this.sources = (sources.isEmpty() ? List.of(".") : sources).stream().map(Source::new)
                    .collect(Collectors.toList());
            this.packages = j.getPackages().stream().map(Package::new).collect(Collectors.toList());
            this.lineRate = j.lineRate();
            this.branchRate = j.branchRate();
            this.complexity = j.complexity();
        }
    }

    @Root(strict = false, name = "line")
    static class Line {
        @Attribute(name = "number", required = false)
        private int number;

        @Attribute(name = "hits", required = false)
        private int hits;

        @Attribute(name = "branch", required = false)
        private boolean branch;

        @Attribute(name = "condition-coverage", required = false)
        private String conditionCoverage;

        @ElementList(name = "conditions", required = false)
        private List<Condition> conditions;

        public Line(Jacoco.Line l) {
            this.number = l.getNr();
            this.hits = l.getCi() > 0 ? 1 : 0;

            if (l.getMb() + l.getCb() > 0) {
                branch = true;

                int percentage = (int) (100 * ((double) l.getCb() / (l.getCb() + l.getMb())));
                conditionCoverage = percentage + "% (" + l.getCb() + "/" + (l.getCb() + l.getMb()) + ")";
                conditions = List.of(new Condition(String.valueOf(percentage) + "%"));
            }
        }
    }

    @Root(strict = false, name = "method")
    static class Method {
        @Attribute(name = "name", required = true)
        private final String name;

        @Attribute(name = "signature", required = true)
        private final String signature;

        @Path("lines")
        @ElementList(name = "lines", required = false, inline = true)
        private final List<Line> lines;

        @Attribute(name = "line-rate", required = false)
        protected double lineRate = 0.0;

        @Attribute(name = "branch-rate", required = false)
        protected double branchRate = 0.0;

        @Attribute(name = "complexity", required = false)
        protected double complexity = 0.0;

        public String getName() {
            return name;
        }

        public List<Line> getLines() {
            return lines;
        }

        public Method(Jacoco.MethodElement m, String jSource, Jacoco.PackageElement jPack) {
            this.name = m.getName() != null ? m.getName() : "";
            this.signature = m.getDesc() != null ? m.getDesc() : "";
            this.lines = linesForMethod(m, jPack, jSource).stream().map(Line::new).collect(Collectors.toList());
            this.lineRate = m.lineRate();
            this.branchRate = m.branchRate();
            this.complexity = m.complexity();
        }
    }

    @Root(strict = false, name = "package")
    static class Package {
        @Attribute(name = "name", required = true)
        private final String name;

        @Path("classes")
        @ElementList(name = "classes", required = false, inline = true)
        private final List<ClassElement> classes;

        @Attribute(name = "line-rate", required = false)
        protected double lineRate = 0.0;

        @Attribute(name = "branch-rate", required = false)
        protected double branchRate = 0.0;

        @Attribute(name = "complexity", required = false)
        protected double complexity = 0.0;

        public Package(Jacoco.PackageElement p) {
            this.name = p.getName() != null ? p.getName() : "";
            this.classes = p.getClasses().stream().map(classElement -> new ClassElement(classElement, p))
                    .collect(Collectors.toList());
            this.lineRate = p.lineRate();
            this.branchRate = p.branchRate();
            this.complexity = p.complexity();
        }
    }

    @Root(strict = false, name = "source")
    static class Source {
        @Text
        private final String value;

        public Source(String s) {
            this.value = s;
        }
    }

}
