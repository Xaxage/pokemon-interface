import type { EvolutionStage } from '../api/types';
import { displayName } from '../format';

/** The evolution chain from its first stage; branches (e.g. Eevee) are shown side by side. */
export function EvolutionTree({ chain, current }: { chain: EvolutionStage | null; current: string }) {
  if (!chain) return <p className="muted">No evolution data.</p>;
  if (chain.evolvesTo.length === 0) return <p className="muted">{displayName(chain.name)} does not evolve.</p>;
  return (
    <ul className="evolution">
      <Stage stage={chain} current={current} />
    </ul>
  );
}

function Stage({ stage, current }: { stage: EvolutionStage; current: string }) {
  return (
    <li>
      <span className={`evolution__stage ${stage.name === current ? 'evolution__stage--current' : ''}`}>
        {displayName(stage.name)}
      </span>
      {stage.evolvesTo.length > 0 && (
        <ul>
          {stage.evolvesTo.map((next) => (
            <Stage key={next.name} stage={next} current={current} />
          ))}
        </ul>
      )}
    </li>
  );
}
