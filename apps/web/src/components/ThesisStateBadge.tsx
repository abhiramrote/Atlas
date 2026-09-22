interface ThesisStateBadgeProps {
  state: string;
  large?: boolean;
}

function stateClass(state: string): string {
  switch (state.toUpperCase()) {
    case "CONFIRMED":
      return "state-confirmed";
    case "EMERGING":
      return "state-emerging";
    case "CANDIDATE":
      return "state-candidate";
    case "WEAKENING":
      return "state-weakening";
    case "INVALIDATED":
      return "state-invalidated";
    case "ARCHIVED":
      return "state-archived";
    default:
      return "state-draft";
  }
}

function ThesisStateBadge({
  state,
  large = false,
}: ThesisStateBadgeProps) {
  const classes = [
    "thesis-state",
    stateClass(state),
    large ? "large" : "",
  ]
    .filter(Boolean)
    .join(" ");

  return <span className={classes}>{state}</span>;
}

export default ThesisStateBadge;
