package com.tomassatka;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class Jacoco {

    interface Counters {
        List<Counter> getCounters();

        default double branchRate() {
            return counter("BRANCH", getCounters());
        }

        default double lineRate() {
            return counter("LINE", getCounters());
        }

        default double complexity() {
            return counter("COMPLEXITY", getCounters(), Counters::sum);
        }

        static double counter(String type, List<Counter> counters) {
            return counter(type, counters, Counters::fraction);
        }

        static double counter(String type, List<Counter> counters, BiFunction<Integer, Integer, Double> op) {
            Optional<Counter> optionalCounter = counters.stream()
                    .filter(counter -> counter.getType().equals(type))
                    .findFirst();

            if (optionalCounter.isPresent()) {
                Counter counter = optionalCounter.get();
                return op.apply(counter.getCovered(), counter.getMissed());
            } else {
                return 0.0;
            }
        }

        static double sum(int s1, int s2) {
            return (double) (s1 + s2);
        }

        static double fraction(int s1, int s2) {
            if (s1 != 0 || s2 != 0) {
                return (double) s1 / (s1 + s2);
            } else {
                return 0.0;
            }
        }
    }

    @Root(strict = false, name = "report")
    static class Report implements Counters {
        @Attribute(name = "name")
        private String name;

        @ElementList(name = "package", required = false, inline = true)
        private List<PackageElement> packages = new ArrayList<>();

        @ElementList(name = "sessioninfo", required = false, inline = true)
        private List<SessionInfo> sessionInfos = new ArrayList<>();

        @ElementList(name = "counter", required = false, inline = true)
        private List<Counter> counters = new ArrayList<>();

        public long timestamp() {
            return sessionInfos.isEmpty() ? 0 : Long.parseLong(sessionInfos.get(0).getStart()) / 1000;
        }

        public List<PackageElement> getPackages() {
            return packages;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<SessionInfo> getSessionInfos() {
            return sessionInfos;
        }

        public void setSessionInfos(List<SessionInfo> sessionInfos) {
            this.sessionInfos = sessionInfos;
        }

        public void setPackages(List<PackageElement> packages) {
            this.packages = packages;
        }

        public java.util.Set<String> packagesNames() {
            java.util.Set<String> packageNames = new java.util.HashSet<>();
            for (PackageElement p : packages) {
                if (p.getName() != null) {
                    packageNames.add(p.getName());
                }
            }
            return packageNames;
        }

        public Jacoco.Report copyReportWithPackage(Jacoco.Report jacocoData, PackageElement packageElement) {
            Jacoco.Report packageData = new Jacoco.Report();

            packageData.setName(jacocoData.getName());
            packageData.setSessionInfos(jacocoData.getSessionInfos());

            List<PackageElement> newPackages = new ArrayList<>();
            newPackages.add(packageElement);
            packageData.setPackages(newPackages);

            return packageData;
        }

        public java.util.List<String> sources() {
            java.util.List<String> sourceList = new ArrayList<>();
            for (PackageElement p : packages) {
                for (SourceFile s : p.getSourceFiles()) {
                    if (s.getName() != null) {
                        sourceList.add((p.getName() != null ? p.getName() : "") + "/" + s.getName());
                    }
                }
            }
            return sourceList;
        }

        @Override
        public List<Counter> getCounters() {
            return counters;
        }

        public void print() {
            System.out.println(name);
            System.out.println(sessionInfos.stream().map(SessionInfo::getId).collect(Collectors.joining(", ")));
            packages.forEach(packageElement -> {
                System.out.println("   package: " + packageElement.getName());
                packageElement.getClasses().forEach(classElement -> {
                    System.out.println(
                            "       class: " + classElement.getName() + " - " + classElement.getSourcefilename());
                    classElement.getMethods().forEach(methodElement -> {
                        System.out.println("         " + methodElement.getName() + " - " + methodElement.getDesc()
                                + " - " + methodElement.getLine());
                        methodElement.getCounters().forEach(counter -> {
                            System.out.println("               " + counter.getType() + " - " + counter.getMissed()
                                    + " - " + counter.getCovered());
                        });
                    });
                });
                packageElement.getSourceFiles().forEach(sourceFile -> {
                    System.out.println("       sourcefile: " + sourceFile.getName());
                    sourceFile.getLines().forEach(line -> {
                        System.out.println(
                                "         line: " + line.getMi() + " - " + line.getCi() + " - " + line.getCb());
                    });
                    sourceFile.getCounters().forEach(counter -> {
                        System.out.println("         counter: " + counter.getType() + " - " + counter.getMissed()
                                + " - " + counter.getCovered());
                    });
                });
                packageElement.getCounters().forEach(counter -> {
                    System.out.println("   counter: " + counter.getType() + " - " + counter.getMissed() + " - "
                            + counter.getCovered());
                });
            });
            counters.forEach(counter -> {
                System.out.println(
                        "counter: " + counter.getType() + " - " + counter.getMissed() + " - " + counter.getCovered());
            });
        }

    }

    @Root(name = "package", strict = false)
    static class PackageElement implements Counters {
        @Attribute(name = "name", required = false)
        private String name;

        @ElementList(name = "class", required = false, inline = true)
        private List<ClassElement> classes = new ArrayList<>();

        @ElementList(name = "sourcefile", required = false, inline = true)
        private List<SourceFile> sourcefiles = new ArrayList<>();

        @ElementList(name = "counter", required = false, inline = true)
        private List<Counter> counters = new ArrayList<>();

        public String getName() {
            return name;
        }

        public List<SourceFile> getSourceFiles() {
            return sourcefiles;
        }

        public List<ClassElement> getClasses() {
            return classes;
        }

        @Override
        public List<Counter> getCounters() {
            return counters;
        }
    }

    @Root(name = "sourcefile", strict = false)
    static class SourceFile implements Counters {
        @Attribute(name = "name", required = false)
        private String name;

        @ElementList(name = "line", required = false, inline = true)
        private List<Line> lines = new ArrayList<>();

        @ElementList(name = "counter", required = false, inline = true)
        private List<Counter> counters = new ArrayList<>();

        public String getName() {
            return name;
        }

        public List<Line> getLines() {
            return lines;
        }

        @Override
        public List<Counter> getCounters() {
            return counters;
        }
    }

    @Root(name = "line", strict = false)
    static class Line {
        @Attribute(name = "nr", required = false)
        private int nr;

        @Attribute(name = "mi", required = false)
        private int mi;

        @Attribute(name = "ci", required = false)
        private int ci;

        @Attribute(name = "mb", required = false)
        private int mb;

        @Attribute(name = "cb", required = false)
        private int cb;

        public int getNr() {
            return nr;
        }

        public int getMi() {
            return mi;
        }

        public int getMb() {
            return mb;
        }

        public int getCi() {
            return ci;
        }

        public int getCb() {
            return cb;
        }
    }

    @Root(name = "class", strict = false)
    static class ClassElement implements Counters {
        @Attribute(name = "name", required = false)
        private String name;

        @Attribute(name = "sourcefilename", required = false)
        private String sourcefilename;

        @ElementList(name = "method", required = false, inline = true)
        private List<MethodElement> methods = new ArrayList<>();

        @ElementList(name = "counter", required = false, inline = true)
        private List<Counter> counters = new ArrayList<>();

        public String getName() {
            return name;
        }

        public List<MethodElement> getMethods() {
            return methods;
        }

        public String getSourcefilename() {
            return sourcefilename;
        }

        @Override
        public List<Counter> getCounters() {
            return counters;
        }

    }

    @Root(name = "method", strict = false)
    static class MethodElement implements Counters {
        @Attribute(name = "name", required = false)
        private String name;

        @Attribute(name = "desc", required = false)
        private String desc;

        @Attribute(name = "line", required = false)
        private Integer line;

        @ElementList(name = "counter", required = false, inline = true)
        private List<Counter> counters = new ArrayList<>();

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        public Integer getLine() {
            return line;
        }

        @Override
        public List<Counter> getCounters() {
            return counters;
        }
    }

    @Root(name = "counter", strict = false)
    static class Counter {
        @Attribute(name = "type", required = false)
        private String type;

        @Attribute(name = "missed", required = false)
        private int missed;

        @Attribute(name = "covered", required = false)
        private int covered;

        public String getType() {
            return type;
        }

        public int getMissed() {
            return missed;
        }

        public int getCovered() {
            return covered;
        }
    }

    @Root(strict = false, name = "sessioninfo")
    static class SessionInfo {
        @Attribute(name = "id", required = false)
        private String id;

        @Attribute(name = "start", required = false)
        private String start;

        @Attribute(name = "dump", required = false)
        private String dump;

        public String getId() {
            return id;
        }

        public String getStart() {
            return start;
        }

        public String getDump() {
            return dump;
        }
    }
}
