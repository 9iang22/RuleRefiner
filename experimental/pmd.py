import os
import sys
import shutil
import logging
import tempfile
import subprocess
import json

jacoco = None
pmd = None


def init():
    global jacoco, pmd, pmd5, pmd6, pmd7
    jacoco = os.getenv('JACOCO_HOME')
    if jacoco is None:
        logging.error('JACOCO_HOME not set')
        sys.exit(1)
    pmd = os.getenv('PMD_HOME')
    if pmd is None:
        logging.error('PMD_HOME not set')
        sys.exit(1)
    pmd5 = os.getenv('PMD5_HOME')
    pmd6 = os.getenv('PMD6_HOME')
    pmd7 = os.getenv('PMD7_HOME')
    return

init()

def run_cmd(command):
    logging.debug(f"run command: {command}")
    # Run the Semgrep command
    result = subprocess.run(command, shell=True, check=False, 
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    # Decode the output from bytes to string
    output = result.stdout.decode('utf-8')
    error = result.stderr.decode('utf-8')
    return result.returncode, output, error

def build_class(rule_path, build_dir):
    with tempfile.TemporaryDirectory(prefix='build_') as build_dir:
        cmd = f"javac -d {build_dir} -cp '{pmd}/lib/*' {rule_path}"
        return run_cmd(cmd)

def unsure_version_build_class(rule_path, build_dir):
    for pmd in [pmd5, pmd6, pmd7]:
        with tempfile.TemporaryDirectory(prefix='build_') as build_dir:
            cmd = f"javac -d {build_dir} -cp '{pmd}/lib/*' {rule_path}"
            rcode, stdout, stderr =  run_cmd(cmd)
            if rcode == 0:
                break
    return rcode, stdout, stderr

def test_pmd_version(rule_path):
    for pmd in [pmd5, pmd6, pmd7]:
        with tempfile.TemporaryDirectory(prefix='build_') as build_dir:
            cmd = f"javac -d {build_dir} -cp '{pmd}/lib/*' {rule_path}"
            rcode, stdout, stderr =  run_cmd(cmd)
            if rcode == 0:
                return pmd
    return None

def build_rule_jar(build_dir, rule_path, output_jar, xpmd=pmd):
        cmd = f"""javac -d {build_dir} -cp '{xpmd}/lib/*' {rule_path} && \\
jar -c -f {output_jar} -C {build_dir} ."""
        # Run the shell script
        return run_cmd(cmd)

# using online instrumentation instead
# def inst_with_jacoco(rule_jar):
#     with tempfile.TemporaryDirectory(prefix='inst_') as build_dir:
#         cmd = f"java -jar {jacoco}/lib/jacococli.jar instrument {rule_jar} --dest {build_dir}"
#         inst_jar = os.path.join(build_dir, os.path.basename(rule_jar))
#         rcode, stdout, stderr = run_cmd(cmd)
#         shutil.copy(rule_jar, rule_jar+".bak")
#         shutil.copy(inst_jar, rule_jar)
#         return rcode, stdout, stderr

def run_pmd_with_jacoco(rule_inst_jar, rule_xml, src_dir, cov_output, xpmd=pmd):
    CLASSPATH = f"{rule_inst_jar}:{jacoco}/lib/*"
    PMD_JAVA_OPTS = f"-javaagent:{jacoco}/lib/jacocoagent.jar=destfile={cov_output}"
    if xpmd == pmd7:
        cmd = f"CLASSPATH={CLASSPATH} PMD_JAVA_OPTS={PMD_JAVA_OPTS} {xpmd}/bin/pmd check --no-cache -f json -d {src_dir} -R {rule_xml}"
    elif xpmd == pmd6:
        cmd = f"CLASSPATH={CLASSPATH} PMD_JAVA_OPTS={PMD_JAVA_OPTS} {xpmd}/bin/run.sh pmd --no-cache -f json -d {src_dir} -R {rule_xml}"
    elif xpmd == pmd5:
        cmd = f"CLASSPATH={CLASSPATH} PMD_JAVA_OPTS={PMD_JAVA_OPTS} {xpmd}/bin/run.sh pmd -f csv -d {src_dir} -R {rule_xml}"
    logging.debug(f"run pmd with jacoco: {cmd}")
    rcode, stdout, stderr = run_cmd(cmd)
    return rcode, stdout, stderr

def run_pmd(rule_jar, rule_xml, src_dir, xpmd=pmd):
    CLASSPATH = f"{rule_jar}"
    PMD_JAVA_OPTS = ""
    if xpmd == pmd7:
        cmd = f"CLASSPATH={CLASSPATH} {xpmd}/bin/pmd check --no-cache -f json -d {src_dir} -R {rule_xml}"
    elif xpmd == pmd6:
        cmd = f"CLASSPATH={CLASSPATH} {xpmd}/bin/run.sh pmd --no-cache -f json -d {src_dir} -R {rule_xml}"
    elif xpmd == pmd5:
        cmd = f"CLASSPATH={CLASSPATH} {xpmd}/bin/run.sh pmd -f csv -d {src_dir} -R {rule_xml}"
    rcode, stdout, stderr = run_cmd(cmd)
    logging.debug(f"run pmd: {cmd}, {rcode}, {stdout}, {stderr}")
    return rcode, stdout, stderr

def dump_report(execfile, original_rule_jar, src, output, mode):
    cmd = f"java -jar {jacoco}/lib/jacococli.jar report {execfile} --classfiles {original_rule_jar} --sourcefiles {src} --{mode} {output}"
    rcode, stdout, stderr = run_cmd(cmd)
    return rcode, stdout, stderr


def make_ruleset(classname, lang):
    return f"""<?xml version="1.0"?>
<ruleset name="My Rule"
    xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 http://pmd.sourceforge.net/ruleset_2_0_0.xsd">
    <description>dummy</description>
    <rule name="Test_Rule"
          language="{lang}"
          class="{classname}"
          message="dummy">
        <description>dummy</description>
        <priority>1</priority>
    </rule>
</ruleset>
"""

def build_test_pipeline(dir, testsrc):
    with tempfile.TemporaryDirectory(prefix='build_') as build_dir:
        out_jar = os.path.join(build_dir, "rule.jar")
        pmd_ver = test_pmd_version(dir)
        build_rule_jar(build_dir, dir, out_jar,xpmd=pmd_ver)
        rcode, stdout, stderr = run_pmd(out_jar, os.path.join(dir, "*.xml"), testsrc)

def pmd_run_in_dir(build_dir, rule_path, test_path, class_name, lang):
    out_jar = os.path.join(build_dir, "rule.jar")
    xml = make_ruleset(class_name, lang)
    with open(os.path.join(build_dir, "rule.xml"), "w") as f:
        f.write(xml)
    pmd_ver = test_pmd_version(rule_path)
    rcode, output, stderr = build_rule_jar(build_dir, rule_path, out_jar,xpmd=pmd_ver)
    if rcode != 0:
        logging.warning("build rule jar failed : {}".format(stderr))
        return rcode, output, "SYNTAX_ERROR"
    rcode, output, stderr = run_pmd(out_jar, os.path.join(build_dir, "rule.xml"), test_path, xpmd=pmd_ver)
    return rcode, output, stderr

def pmd_run_in_temp_dir(rule, test, rp, tp, class_name, lang):
    with tempfile.TemporaryDirectory(prefix='build_') as build_dir:
        rule_path = os.path.join(build_dir, os.path.basename(rp))
        with open(rule_path, "w") as f:
            f.write(rule)
        test_path = os.path.join(build_dir, os.path.basename(tp))
        with open(test_path, "w") as f:
            f.write(test)
        return pmd_run_in_dir(build_dir, rule_path, test_path, class_name, lang)

def pmd_analysis_in_dir(build_dir, rule_path, test_path, class_name, lang, output_xml):
    out_jar = os.path.join(build_dir, "rule.jar")
    cn = class_name
    xml = make_ruleset(cn, lang)
    with open(os.path.join(build_dir, "rule.xml"), "w") as f:
        f.write(xml)
    pmd_ver = test_pmd_version(rule_path)
    rcode, output, stderr = build_rule_jar(build_dir, rule_path, out_jar,xpmd=pmd_ver)
    if rcode != 0:
        logging.warning("build rule jar failed : {}".format(stderr))
        return rcode, output, "SYNTAX_ERROR"
    rcode, output, stderr = run_pmd_with_jacoco(out_jar, os.path.join(build_dir, "rule.xml"), test_path, os.path.join(build_dir, "jacoco.exec"), xpmd=pmd_ver)
    if rcode != 0 and rcode != 4:
        logging.warning("run pmd failed : {}".format(stderr))
        return rcode, output, "COMMAND_ERROR"
    rcode, output, stderr = dump_report(os.path.join(build_dir, "jacoco.exec"), out_jar, rule_path, output_xml, "xml")
    if rcode != 0:
        logging.warning("dump report failed : {}".format(stderr))
        return rcode, output, "COMMAND_ERROR"
    return 0, open(output_xml).read(), ""

def pmd_analysis_in_temp_dir(rule, test, rp, tp, class_name, lang, output_xml):
    with tempfile.TemporaryDirectory(prefix='build_') as build_dir:
        rule_path = os.path.join(build_dir, os.path.basename(rp))
        with open(rule_path, "w") as f:
            f.write(rule)
        test_path = os.path.join(build_dir, os.path.basename(tp))
        with open(test_path, "w") as f:
            f.write(test)
        return pmd_analysis_in_dir(build_dir, rule_path, test_path, class_name, lang, output_xml)

def test():
    global pmd5, pmd6, pmd7
    pmd5 = "./graph/vendor/pmd-bin-5.8.1"
    pmd6 = "./graph/vendor/pmd-bin-6.55.0"
    pmd7 = "./graph/vendor/pmd-bin-7.10.0"
    pmd_dataset = "./pmd_dataset"
    import concurrent.futures
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as executor:
            afters = [os.path.join(pmd_dataset, hash, "before") for hash in os.listdir(pmd_dataset)]
            futures = [executor.submit(test_pmd_version, after) for after in afters]
            for future in concurrent.futures.as_completed(futures):
                    print(future.result())

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s', stream=sys.stdout)
    rule_path = "./examples/493e0014b0/after/UnnecessaryFullyQualifiedNameRule.java"
    test_path = "./examples/493e0014b0/after/FQNTest.java"
    build_dir = "./examples/493e0014b0/build"
    if os.path.exists(build_dir):
        shutil.rmtree(build_dir)
    os.makedirs(build_dir)

    out_jar = os.path.join(build_dir, "rule.jar")
    cn = "net.sourceforge.pmd.lang.java.rule.codestyle.UnnecessaryFullyQualifiedNameRule"
    rcode, coverage_xml, _ = pmd_analysis_in_temp_dir(rule_path, test_path, cn, os.path.join(build_dir, "coverage.xml"))
    print(coverage_xml)

        
    