import logging
from semgrep import semgrep_test_in_tempdir, OK, SYNTAXERROR, COMMANDERROR

def syntax_check(new_rule, e):
    try:
        ok, msg = semgrep_test_in_tempdir(new_rule, e.content, e.rname, e.tname)
        return ok == OK
    except:
        return False

def regression(new_rule, old_rule, example_set):
    correct_set = [e for e in example_set if e.ok()]
    for e in correct_set:
        try:
            ok, msg = semgrep_test_in_tempdir(new_rule, e.content, e.rname, e.tname)
            assert ok == OK
            from semgrep_output_parser import analysis_semgrep_output
            ret = analysis_semgrep_output(msg)
            if ret == None or ret['passed'] == False:
                return False
        except Exception as e:
            logging.error(f"regression check failed: {e}")
            return False
    return True

def verify_fix(new_rule, incorrect):
    try:
        ok, msg = semgrep_test_in_tempdir(new_rule, incorrect.content, incorrect.rname, incorrect.tname)
        assert ok == OK
        from semgrep_output_parser import analysis_semgrep_output
        ret = analysis_semgrep_output(msg)
        return ret['passed']
    except Exception as e:
        logging.error(f"verify fix failed: {e}")
        return False