# RuleRefiner
This repository is for the artifacts of the paper “Fact-Aligned and Template-Constrained Static Analyzer Rule Enhancement with LLMs”

## Description

RuleRefiner introduces a multi-stage LLM framework that eliminates false alarms in static analyzers by refining detection rules through dynamic profiling, differential fault localization, and constrained LLM modifications, achieving 80.28% success on 218 Semgrep issues (1.34x-2.45x over baselines) while producing expert-level rules.

---

## Case Study
We have placed a case study [here](./case_study.md).

## Environment Setup
### Prerequisites
- Python 3.10+
- pip 22.0+

### Installation

#### Step1: Install dependencies
```bash
# Install dependencies
pip install -r requirements.txt
```
#### Step2: set api key for LLMs
For DeepSeek
```bash
export DS_APIKEY=XXXXXXXXXXXXXX
```

For Qwen
```bash
export DASHSCOPE_API_KEY=XXXXXXXXXXXXXXXX
```

For more detail or to support other LLMs out of our paper, please refers to `deepseek.py`.
```bash
cat deepseek.py
```

#### Step 3: Vaildate the environment

Run the pipeline test, it should work without exception.
```bash
python3 semgrep_pipeline_test.py
```
---

## CLI Description

Here's the CLI documentation for your Semgrep pipeline script in markdown format:

### Command
```bash
python semgrep_pipeline.py [OPTIONS]
```

### Description
Runs the Semgrep rule refinement pipeline with configurable modes and models. Executes the pipeline `k` times (pass@k evaluation), aggregates results, and calculates success rate.

---

### Arguments
| Argument | Type | Default | Choices | Description |
|----------|------|---------|---------|-------------|
| `--mode` | str | `full` | `naive`, `cot`, `fewshot`, `localization`, `template`, `full` | Pipeline execution mode:<br>- `naive`: Basic LLM refinement<br>- `cot`: Chain-of-Thought prompting<br>- `fewshot`: Few-shot learning<br>- `localization`: Fault localization only<br>- `template`: Template-guided refinement<br>- `full`: Full RuleRefiner pipeline |
| `--prompt_file` | str | `results/semgrep_prompts.jsonl` | - | Output file for generated LLM prompts |
| `--result_file` | str | `results/semgrep_result.jsonl` | - | Output file for raw LLM responses |
| `--verify_file` | str | `results/semgrep_verify.jsonl` | - | Output file for verification results |
| `--temperature` | float | `0.0` | `0.0-1.0` | LLM sampling temperature (0=deterministic) |
| `--model` | str | `deepseek-v3` | `deepseek-v3`, `qwen-plus` | LLM backend to use |
| `--k` | int | `1` | `1-10` | Number of iterations for pass@k evaluation |

---

## Pipeline Execution Flow
1. **Initialization**:
   - Loads dataset from `dataset/semgrep.jsonl`
   - Configures selected LLM backend with specified temperature
   - Sets up logging

2. **Pipeline Execution**:
   ```python
   for i in range(1, args.k+1):
       pipeline(
           data=data,
           mode=args.mode,
           prompt_file=f"{args.prompt_file}.{i}",
           result_file=f"{args.result_file}.{i}",
           verify_file=f"{args.verify_file}.{i}"
       )
   ```

3. **Result Aggregation**:
   - Combines results from all `k` iterations
   - Calculates pass@k success rate

4. **Output**:
   - Prints final success rate: `Pass@{k}: {success_count}/{total_rules}`
   - Generates output files for each iteration:
     - `{prompt_file}.{i}`
     - `{result_file}.{i}`
     - `{verify_file}.{i}`

---

### Example Usage

**Basic execution with default parameters:**
```bash
python semgrep_pipeline.py
```

**Full pipeline with Qwen model:**
```bash
python semgrep_pipeline.py \
  --mode full \
  --model qwen-plus \
  --temperature 0.3 \
  --k 5
```

**Run localization-only mode:**
```bash
python semgrep_pipeline.py \
  --mode localization \
  --prompt_file results/localization_prompts.jsonl
```

**Evaluate few-shot learning with pass@3:**
```bash
python semgrep_pipeline.py \
  --mode fewshot \
  --k 3 \
  --result_file results/fewshot_results.jsonl
```

---

### Output Files
| File Pattern | Contents |
|-------------|----------|
| `results/semgrep_prompts.jsonl.{i}` | Generated prompts for each rule |
| `results/semgrep_result.jsonl.{i}` | Raw LLM responses |
| `results/semgrep_verify.jsonl.{i}` | Verification results with success status |

---

## Research Questions

### RQ1
The following command start a evaluation pipeline on all 218 problems in the dataset,
with `model=deepseek-v3` and `temperature=0.0`, output the pass@1 result in `results/semgrep_verify.jsonl.1`:
```bash
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1
```
In RQ1, the following setting are evaluated.
```bash
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1 
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 5
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 1 
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 5

python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 1 
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 5
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 1 
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 5
```

<span style="background-color: #f20000">**The results will be overwrite if `verify_file` is set as default**</span>

### RQ2
The baselines are naive (basic prompt), cot (CoT prompt), fewshot (Fewshot prompt) mode.
```bash
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1 --mode naive
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1 --mode cot
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1 --mode fewshot

python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 1 --mode naive
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 1 --mode cot
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 1 --mode fewshot

python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1 --mode naive
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 5 --mode cot
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 5 --mode fewshot

python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 5 --mode naive
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 5 --mode cot
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 5 --mode fewshot

# For qwen-plus
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 1 --mode naive
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 1 --mode cot
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 1 --mode fewshot

python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 1 --mode naive
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 1 --mode cot
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 1 --mode fewshot

python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 1 --mode naive
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 5 --mode cot
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 5 --mode fewshot

python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 5 --mode naive
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 5 --mode cot
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 5 --mode fewshot
```
### RQ3 Ablation Study

```bash
# full feature
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1 --mode full
# without template-constrained
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1 --mode localization
# without differential-localization and template-constrained
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 0.0 --k 1 --mode naive

# full feature
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 1 --mode full
# without template-constrained
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 1 --mode localization
# without differential-localization and template-constrained
python3 semgrep_pipeline.py --model deepseek-v3 --temperature 1.0 --k 1 --mode naive

# full feature
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 1 --mode full
# without template-constrained
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 1 --mode localization
# without differential-localization and template-constrained
python3 semgrep_pipeline.py --model qwen-plus --temperature 0.0 --k 1 --mode naive

# full feature
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 1 --mode full
# without template-constrained
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 1 --mode localization
# without differential-localization and template-constrained
python3 semgrep_pipeline.py --model qwen-plus --temperature 1.0 --k 1 --mode naive
```

### RQ4: Generalization Capability Evaluation

To assess the generalization capabilities of refined rules, we implemented a mutation-based testing framework using [Statfier](https://github.com/cshezhang/Statfier) with the following workflow:

#### 1. **Seed Selection**
Selected 24 critical Java security rules as mutation seeds:
```yaml
- security_command-injection-formatted-runtime-call
- security_command-injection-process-builder
- security_cookie-missing-samesite
- security_detect-child-process
- security_detect-non-literal-fs-filename
- security_detect-non-literal-regexp
- security_el-injection
- security_express-puppeteer-injection
# ... (full list in repository)ontext
```

And mutate them following the guidance: https://github.com/cshezhang/Statfier.
The variants are then send to semgrep validator to check if the refined rule can still correctly identify them.

---

## Result Analysis
After running `semgrep_pipeline.py`, the following outputs will be generated:

1. **Summary Statistics**:
   ```bash
   Pass@{k}: {success_count} / {total_samples}
   ```
   - Shows the total number of rules that passed all verification checks

2. **Detailed Results**:
   - Saved in JSON Lines format at: `results/semgrep_verify.jsonl.*`
   - Each line represents one rule refinement attempt
   - Only samples with `["REFINE_SUCCESS"]` status are counted as passed

### Verification Status Codes
| Combination | Meaning | Success | Description |
|-------------|---------|---------|-------------|
| `["REFINE_SUCCESS"]` | ✓ | Yes | Passed both defect-revealing case <u>and</u> regression tests |
| `["REFINE_FAILED", "REGRESSION_SUCCESS"]` | ✗ | No | Failed defect-revealing case (false negative) but passed regression |
| `["REFINE_SUCCESS", "REGRESSION_FAILED"]` | ✗ | No | Passed defect case but failed regression (introduced new false positive) |
| `["REFINE_FAILED", "REGRESSION_FAILED"]` | ✗ | No | Failed both defect case and regression tests |
| `["SYNTAX_ERROR"]` | ✗ | No | Generated rule contains syntax errors |
```

## Project Structure
```bash
├── dataset/                   # Semgrep datasets
├── examples/                  # Example rule files and test cases
├── experimental/              # Experimental predicate graph translation for CodeQL and PMD
├── results/                   # Output files and evaluation results
├── scripts/                   # useful scripts
├── venv/                      # Virtual environment (excluded from version control)
│
├── semgrep2nx.py               # Semgrep to Predicate Graph conversion & dynamic profiling
├── graph.py                    # Graph analysis 
├── semgrep_locate.py           # Fault localization implementation  
├── semgrep_template.py         # template generator
├── semgrep_prompt.py           # prompt generation
├── semgrep_verify.py           # Rule validation                     
├── semgrep_pipeline.py         # pipeline
├── semgrep_syntax.py           # Rule syntax fixer
└── testcase.py                 # Test case
│
├── models/                    # LLM integration modules
│   ├── deepseek.py            # DeepSeek model interface
│   ├── doubao.py              # Doubao model interface
│   └── qwen.py                # Qwen model interface
│
├── semgrep_output_parser.py    # Output parsing utilities
├── utils.py                    # Common utilities
└── para.py                     # Parallel processing config
...
```