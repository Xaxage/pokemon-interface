import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import type { EditStoredPokemonRequest, StoredPokemon } from '../api/types';
import { TagInput } from './TagInput';

// Same limits the backend enforces (EditStoredPokemonRequest); checked here first for instant feedback.
const MAX_NAME = 50;
const MAX_REGION = 50;
const MAX_TAGS = 10;
const MAX_TAG = 30;

type Field = 'localizedName' | 'region' | 'tags';
type Errors = Partial<Record<Field, string>>;

interface StoredPokemonFormProps {
  pokemon: StoredPokemon;
  submitting: boolean;
  serverError: unknown;
  onSubmit: (fields: EditStoredPokemonRequest) => void;
  onCancel: () => void;
}

function validate(localizedName: string, region: string, tags: string[]): Errors {
  const errors: Errors = {};
  if (localizedName.length > MAX_NAME) errors.localizedName = `At most ${MAX_NAME} characters.`;
  if (region.length > MAX_REGION) errors.region = `At most ${MAX_REGION} characters.`;
  if (tags.length > MAX_TAGS) errors.tags = `At most ${MAX_TAGS} tags.`;
  else if (tags.some((tag) => tag.length > MAX_TAG)) errors.tags = `Each tag is at most ${MAX_TAG} characters.`;
  return errors;
}

/** Maps the backend's invalidFields (e.g. "tags[2]") onto the form's fields. */
function serverErrors(error: unknown): Errors {
  if (!(error instanceof ApiError)) return {};
  const errors: Errors = {};
  for (const { field, reason } of error.invalidFields) {
    const key = field.replace(/\[\d+]$/, '') as Field;
    errors[key] ??= reason;
  }
  return errors;
}

/** Edits our own fields of a stored Pokémon. Blank fields are left out, which the backend treats as "clear". */
export function StoredPokemonForm({ pokemon, submitting, serverError, onSubmit, onCancel }: StoredPokemonFormProps) {
  const [localizedName, setLocalizedName] = useState(pokemon.localizedName ?? '');
  const [region, setRegion] = useState(pokemon.region ?? '');
  const [tags, setTags] = useState<string[]>(pokemon.tags);
  const [touched, setTouched] = useState(false);

  const clientErrors = validate(localizedName.trim(), region.trim(), tags);
  const errors: Errors = { ...serverErrors(serverError), ...(touched ? clientErrors : {}) };
  const generalError =
    serverError && !(serverError instanceof ApiError && serverError.invalidFields.length > 0)
      ? (serverError as Error).message
      : null;

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (Object.keys(clientErrors).length > 0) return;
    onSubmit({
      localizedName: localizedName.trim() || undefined,
      region: region.trim() || undefined,
      tags: tags.length > 0 ? tags : undefined,
    });
  };

  return (
    <form className="form" onSubmit={handleSubmit} noValidate>
      {generalError && (
        <p className="form__error" role="alert">
          {generalError}
        </p>
      )}

      <div className="field">
        <label htmlFor="localizedName">Localized name</label>
        <input
          id="localizedName"
          value={localizedName}
          maxLength={MAX_NAME}
          placeholder="e.g. Bisasam"
          aria-invalid={!!errors.localizedName}
          aria-describedby="localizedName-hint"
          onChange={(event) => setLocalizedName(event.target.value)}
        />
        <p id="localizedName-hint" className={errors.localizedName ? 'field__error' : 'field__hint'}>
          {errors.localizedName ?? `Optional, up to ${MAX_NAME} characters.`}
        </p>
      </div>

      <div className="field">
        <label htmlFor="region">Region</label>
        <input
          id="region"
          value={region}
          maxLength={MAX_REGION}
          placeholder="e.g. Kanto"
          aria-invalid={!!errors.region}
          aria-describedby="region-hint"
          onChange={(event) => setRegion(event.target.value)}
        />
        <p id="region-hint" className={errors.region ? 'field__error' : 'field__hint'}>
          {errors.region ?? `Optional, up to ${MAX_REGION} characters.`}
        </p>
      </div>

      <div className="field">
        <label htmlFor="tags">Tags</label>
        <TagInput
          id="tags"
          tags={tags}
          max={MAX_TAGS}
          maxLength={MAX_TAG}
          onChange={setTags}
          invalid={!!errors.tags}
          describedBy="tags-hint"
        />
        <p id="tags-hint" className={errors.tags ? 'field__error' : 'field__hint'}>
          {errors.tags ?? `Press Enter or comma to add. Up to ${MAX_TAGS} unique tags, ${MAX_TAG} characters each.`}
        </p>
      </div>

      <p className="muted small">Leaving a field empty clears it.</p>

      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="button" disabled={submitting}>
          {submitting ? 'Saving…' : 'Save changes'}
        </button>
      </div>
    </form>
  );
}
