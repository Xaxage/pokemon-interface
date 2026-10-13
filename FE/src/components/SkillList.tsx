import { useState } from 'react';
import type { Skill } from '../api/types';
import { displayName } from '../format';

function howLearned(skill: Skill): string {
  return skill.methods
    .map((method) => (method.level ? `${displayName(method.method)} (Lv ${method.level})` : displayName(method.method)))
    .join(', ');
}

/** The Pokémon's moves, collapsed by default; the list is only rendered once opened, as it can run to 100+ entries. */
export function SkillList({ skills }: { skills: Skill[] }) {
  const [open, setOpen] = useState(false);

  if (skills.length === 0) return <p className="muted small">No known skills</p>;

  return (
    <div className="skills">
      <button type="button" className="skills__toggle" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
        {open ? '▾' : '▸'} {skills.length} skill{skills.length === 1 ? '' : 's'}
      </button>
      {open && (
        <ul className="skills__list">
          {skills.map((skill) => (
            <li key={skill.name}>
              <span>{displayName(skill.name)}</span>
              <span className="muted">{howLearned(skill)}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
