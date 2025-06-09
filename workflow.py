"""Main LangGraph workflow orchestrator for the agentic development system."""

from typing import Dict, Any, List, TypedDict
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
import json
from pathlib import Path

from prompt_processor import PromptProcessor
from plan_generator import PlanGenerator
from code_generator import CodeGenerator
from file_creator import FileCreator
from config import Config
from response_logger import init_logger, get_logger


class WorkflowState(TypedDict):
    """State object for the LangGraph workflow."""
    # Input processing
    input_path: str
    processed_input: Dict[str, Any]

    # Plan generation
    development_plan: Dict[str, Any]

    # Code generation
    code_generation_results: Dict[str, Any]
    generated_files: List[Dict[str, Any]]

    # File creation
    file_creation_summary: Dict[str, Any]

    # Workflow control
    current_step: str
    errors: List[str]
    is_complete: bool


class AgenticWorkflow:
    """Main workflow orchestrator using LangGraph."""

    def __init__(self):
        self.config = Config()
        self.prompt_processor = PromptProcessor()

        # Initialize response logger first
        self.logger = init_logger()

        # Pass logger to plan generator to avoid duplicate sessions
        self.plan_generator = PlanGenerator(logger=self.logger)

        # Initialize code generation components if enabled
        if self.config.ENABLE_CODE_GENERATION:
            self.code_generator = CodeGenerator(logger=self.logger)
            self.file_creator = FileCreator(logger=self.logger)
        else:
            self.code_generator = None
            self.file_creator = None

        # Initialize the workflow graph
        self.workflow = self._create_workflow()
        self.memory = MemorySaver()
        self.app = self.workflow.compile(checkpointer=self.memory)

    def _create_workflow(self) -> StateGraph:
        """Create the LangGraph workflow."""
        workflow = StateGraph(WorkflowState)

        # Add nodes for each step
        workflow.add_node("process_input", self._process_input_node)
        workflow.add_node("generate_plan", self._generate_plan_node)
        workflow.add_node("generate_code", self._generate_code_node)
        workflow.add_node("create_files", self._create_files_node)

        # Define the workflow edges
        workflow.set_entry_point("process_input")

        # Direct edge from input processing to plan generation
        workflow.add_edge("process_input", "generate_plan")

        # Conditional edge for code generation
        workflow.add_conditional_edges(
            "generate_plan",
            self._should_generate_code,
            {
                "generate": "generate_code",
                "skip": END
            }
        )

        workflow.add_edge("generate_code", "create_files")
        workflow.add_edge("create_files", END)

        return workflow

    def _process_input_node(self, state: WorkflowState) -> WorkflowState:
        """Process the input (text, file, folder, etc.)."""
        try:
            self.logger.log_workflow(f"🔍 Processing input: {state['input_path']}")

            processed_input = self.prompt_processor.process_input(state["input_path"])

            state["processed_input"] = processed_input
            state["current_step"] = "input_processed"

            self.logger.log_success(
                f"Input processed - Type: {processed_input['type']}, Source: {processed_input['metadata'].get('source', 'unknown')}",
                "input_processing"
            )

            return state

        except Exception as e:
            error_msg = f"Input processing error: {str(e)}"
            state["errors"].append(error_msg)
            state["current_step"] = "error"
            self.logger.log_error(error_msg, "input_processing")
            return state



    def _generate_plan_node(self, state: WorkflowState) -> WorkflowState:
        """Generate development plan using Claude."""
        try:
            self.logger.log_workflow("🎯 Starting development plan generation...")

            # Generate plan from processed input
            plan = self.plan_generator.generate_plan(state["processed_input"])

            state["development_plan"] = plan
            state["current_step"] = "plan_generated"

            # Save the plan to a separate file
            plan_file = self.logger.session_dir / "development_plan.json"
            with open(plan_file, 'w', encoding='utf-8') as f:
                json.dump(plan, f, indent=2, ensure_ascii=False)

            # Generate and save tree structure
            try:
                tree_structure = self.plan_generator.generate_tree_structure(plan)
                tree_file = self.logger.session_dir / "project_structure.txt"
                with open(tree_file, 'w', encoding='utf-8') as f:
                    f.write(tree_structure)

                self.logger.log_workflow(f"🌳 Project tree structure saved to: project_structure.txt")
            except Exception as e:
                self.logger.log_error(f"Failed to generate tree structure: {e}", "tree_generation")
                tree_structure = "Tree structure generation failed"

            # Generate and save code generation analysis
            try:
                code_analysis = self.plan_generator.analyze_code_generation_plan(plan)
                analysis_file = self.logger.session_dir / "code_generation_analysis.json"
                with open(analysis_file, 'w', encoding='utf-8') as f:
                    json.dump(code_analysis, f, indent=2, ensure_ascii=False)

                self.logger.log_workflow(f"📊 Code generation analysis saved to: code_generation_analysis.json")

                # Log key insights
                self.logger.log_workflow(f"📋 Code Generation Insights:")
                self.logger.log_workflow(f"   Strategy: {code_analysis['implementation_strategy']}")
                self.logger.log_workflow(f"   Total Files: {code_analysis['total_files']}")
                self.logger.log_workflow(f"   Generation Order: {' → '.join(code_analysis['generation_order'])}")
                self.logger.log_workflow(f"   High Priority Files: {len(code_analysis['files_by_priority']['high'])}")

            except Exception as e:
                self.logger.log_error(f"Failed to generate code analysis: {e}", "code_analysis")

            # Display plan summary
            summary = self.plan_generator.format_plan_summary(plan)
            self.logger.log_workflow("📋 Plan Summary:")
            for line in summary.split('\n'):
                if line.strip():
                    self.logger.log_workflow(f"   {line}")

            # Display tree structure
            self.logger.log_workflow("\n🌳 Project Structure:")
            for line in tree_structure.split('\n'):
                self.logger.log_workflow(f"   {line}")

            self.logger.log_success(f"Development plan generated and saved to development_plan.json", "plan_generation")

            return state

        except Exception as e:
            error_msg = f"Plan generation error: {str(e)}"
            state["errors"].append(error_msg)
            state["current_step"] = "error"
            self.logger.log_error(error_msg, "plan_generation")
            return state

    def _generate_code_node(self, state: WorkflowState) -> WorkflowState:
        """Generate code based on the development plan."""
        try:
            if not self.config.ENABLE_CODE_GENERATION or not self.code_generator:
                self.logger.log_workflow("⏭️ Code generation disabled, skipping...")
                state["current_step"] = "code_generation_skipped"
                state["code_generation_results"] = {"skipped": True}
                return state

            self.logger.log_workflow("🔨 Starting code generation...")

            # Generate code from the development plan
            generation_results = self.code_generator.generate_code_from_plan(state["development_plan"])

            state["code_generation_results"] = generation_results
            state["current_step"] = "code_generated"

            # Save generation results
            results_file = self.logger.session_dir / "code_generation_results.json"
            with open(results_file, 'w', encoding='utf-8') as f:
                json.dump(generation_results, f, indent=2, ensure_ascii=False)

            self.logger.log_workflow(f"📊 Code generation results saved to: code_generation_results.json")

            # Log generation statistics
            total_files = generation_results.get("total_files", 0)
            successful_files = generation_results.get("successful_files", 0)
            success_rate = generation_results.get("success_rate", 0)

            self.logger.log_workflow(f"📈 Generation Statistics:")
            self.logger.log_workflow(f"   Total Files: {total_files}")
            self.logger.log_workflow(f"   Successful: {successful_files}")
            self.logger.log_workflow(f"   Success Rate: {success_rate:.1%}")

            if generation_results.get("success", False):
                self.logger.log_success("Code generation completed successfully", "code_generation")
            else:
                self.logger.log_workflow("⚠️ Code generation completed with errors")
                for error in generation_results.get("errors", [])[:3]:  # Show first 3 errors
                    self.logger.log_workflow(f"   Error: {error}")

            return state

        except Exception as e:
            error_msg = f"Code generation error: {str(e)}"
            state["errors"].append(error_msg)
            state["current_step"] = "error"
            self.logger.log_error(error_msg, "code_generation")
            return state

    def _create_files_node(self, state: WorkflowState) -> WorkflowState:
        """Create physical files from generated code."""
        try:
            if not self.config.ENABLE_CODE_GENERATION or not self.file_creator:
                self.logger.log_workflow("⏭️ File creation disabled, skipping...")
                state["current_step"] = "file_creation_skipped"
                state["file_creation_summary"] = {"skipped": True}
                state["is_complete"] = True
                return state

            generation_results = state.get("code_generation_results", {})

            if not generation_results.get("success", False):
                self.logger.log_workflow("❌ Code generation failed, skipping file creation")
                state["current_step"] = "file_creation_skipped"
                state["file_creation_summary"] = {"skipped": True, "reason": "code_generation_failed"}
                state["is_complete"] = True
                return state

            self.logger.log_workflow("📁 Creating files from generated code...")

            # Create files from generation results
            creation_summary = self.file_creator.create_files_from_generation(generation_results)

            state["file_creation_summary"] = creation_summary.__dict__
            state["current_step"] = "files_created"

            # Save creation summary
            summary_file = self.logger.session_dir / "file_creation_summary.json"
            with open(summary_file, 'w', encoding='utf-8') as f:
                json.dump(creation_summary.__dict__, f, indent=2, ensure_ascii=False)

            # Generate and save creation report
            creation_report = self.file_creator.get_creation_report(creation_summary)
            report_file = self.logger.session_dir / "creation_report.txt"
            with open(report_file, 'w', encoding='utf-8') as f:
                f.write(creation_report)

            self.logger.log_workflow(f"📊 File creation summary saved to: file_creation_summary.json")
            self.logger.log_workflow(f"📋 Creation report saved to: creation_report.txt")

            # Log creation statistics
            self.logger.log_workflow(f"📈 File Creation Statistics:")
            self.logger.log_workflow(f"   Total Files: {creation_summary.total_files}")
            self.logger.log_workflow(f"   Created: {creation_summary.created_files}")
            self.logger.log_workflow(f"   Failed: {creation_summary.failed_files}")
            self.logger.log_workflow(f"   Total Size: {creation_summary.total_size:,} bytes")
            self.logger.log_workflow(f"   Creation Time: {creation_summary.creation_time:.2f}s")

            if creation_summary.created_files > 0:
                self.logger.log_success(f"Created {creation_summary.created_files} files successfully", "file_creation")
                self.logger.log_workflow(f"📁 Output directory: {self.file_creator.output_dir}")
            else:
                self.logger.log_workflow("⚠️ No files were created")

            state["is_complete"] = True
            return state

        except Exception as e:
            error_msg = f"File creation error: {str(e)}"
            state["errors"].append(error_msg)
            state["current_step"] = "error"
            self.logger.log_error(error_msg, "file_creation")
            return state





    def _should_generate_code(self, state: WorkflowState) -> str:
        """Determine if code should be generated."""
        if self.config.ENABLE_CODE_GENERATION and self.code_generator:
            return "generate"
        else:
            return "skip"



    def run(self, input_path: str) -> Dict[str, Any]:
        """
        Run the complete agentic workflow.

        Args:
            input_path: Path to input (file, folder, or direct text)

        Returns:
            Final workflow state
        """
        self.logger.log_workflow("🚀 Starting Agentic Workflow")
        self.logger.log_workflow(f"   Input: {input_path}")
        self.logger.log_workflow(f"   Session ID: {self.logger.session_id}")
        self.logger.log_workflow("-" * 50)

        # Initialize state
        initial_state = WorkflowState(
            input_path=input_path,
            processed_input={},
            development_plan={},
            code_generation_results={},
            generated_files=[],
            file_creation_summary={},
            current_step="starting",
            errors=[],
            is_complete=False
        )

        # Run the workflow
        try:
            config = {"configurable": {"thread_id": "main_workflow"}}
            final_state = self.app.invoke(initial_state, config)

            self.logger.log_workflow("-" * 50)
            self.logger.log_success("Workflow completed!", "workflow")
            self.logger.log_workflow(f"   Final step: {final_state['current_step']}")
            self.logger.log_workflow(f"   Errors: {len(final_state['errors'])}")

            return final_state

        except Exception as e:
            error_msg = f"Workflow failed: {e}"
            self.logger.log_error(error_msg, "workflow")
            return {"error": str(e), "state": initial_state}


def main():
    """Main entry point for testing the workflow."""
    import sys

    if len(sys.argv) < 2:
        print("Usage: python workflow.py <input_path>")
        print("Example: python workflow.py 'Create a simple web calculator'")
        sys.exit(1)

    input_path = sys.argv[1]

    # Create and run workflow
    workflow = AgenticWorkflow()
    result = workflow.run(input_path)

    # Results are automatically saved by the logger
    logger = get_logger()

    print(f"\n📄 All results saved to session: {logger.session_id}")
    print(f"📁 Session directory: {logger.session_dir}")
    print(f"📊 Total LLM interactions: {len(logger.llm_interactions)}")
    print(f"📝 Total log entries: {len(logger.workflow_logs)}")


if __name__ == "__main__":
    main()
