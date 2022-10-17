import com.kerbaya.preserve.PomUtils

PomUtils.assertBuildNumber("1", 1L, basedir);

PomUtils.match("1-\\d{8}\\.\\d{6}-1", "/pom:project/pom:parent/pom:version[text()]", basedir, "child");
PomUtils.assertBuildNumber("2", 1L, basedir, "child");

PomUtils.match("2-\\d{8}\\.\\d{6}-1", "/pom:project/pom:parent/pom:version[text()]", basedir, "child", "grandchild");
PomUtils.assertBuildNumber("3", 1L, basedir, "child", "grandchild");
