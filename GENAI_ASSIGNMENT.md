# GenAI Assignment: Task Management API

**Tool:** Claude Code, the same tool I used to build this whole repo.

## 1. The prompt

I don't put everything into one big prompt. When I've tried that, I get code that compiles but has the logic
inside the controllers and no real tests. What I do instead is put the project rules in a `CLAUDE.md` once,
and then drive the work with a few custom slash commands I wrote (they're in `.claude/commands/`):

- `/discover`: turns a user story into rules with examples and counter-examples, and asks me about anything unclear
- `/accept`: writes one failing acceptance test for one rule, then stops
- `/tdd`: does one red → green → refactor cycle, suggests an edge case, then stops
- `/review`: reviews the diff for architecture and test quality before I commit

The `CLAUDE.md` for this task would be the same as this repo's, with the domain swapped:

```markdown
Spring Boot 4 / Java 21 REST API for a task manager. Users CRUD their own tasks (title, description, status, due_date).
A User model and JWT sign-in already exist.

- Hexagonal: domain is pure Java (no Spring/JPA), controllers only delegate,
  JPA entities never leave the persistence adapter. Records for DTOs, no Lombok.
- @Valid on bodies. 201 create, 200 read/update, 204 delete, 400 invalid, 401 no token, 404 not found.
- One error body everywhere (ProblemDetail). Never leak stack traces or internals.
- ATDD + TDD: one failing test at a time, never edit a failing test to make it pass.
```

Then the actual prompt:

```text
/discover "As a signed-in user I want to manage my tasks: create, see, change and delete them.
A task has a title, description, status (to do / in progress / done) and a due date.
I only see and touch my own tasks. Someone else's task shouldn't even look like it exists to me.
Title is required, description isn't, new tasks start as TO_DO, and you can't create a task
that's already overdue."
```

After that it's just a loop for each rule: `/accept "<rule>" @doc/specs/tasks.md` → `/tdd` a few times →
`/review` → commit.

## 2. Output (sample)

The domain decides what a valid task is:

```java
public record Task(TaskId id, UserId owner, String title, String description,
                   TaskStatus status, LocalDate dueDate) {

    public static Task create(UserId owner, String title, String description,
                              LocalDate dueDate, LocalDate today) {
        if (dueDate.isBefore(today)) {
            throw new TaskAlreadyOverdueException(dueDate);
        }
        return new Task(null, owner, title, description, TaskStatus.TO_DO, dueDate);
    }
}
```

The controller stays thin, and the owner always comes from the token:

```java

@PostMapping
ResponseEntity<TaskResponse> create(@AuthenticationPrincipal Jwt user,
                                    @Valid @RequestBody CreateTaskRequest request) {
    var task = createTask.create(UserId.of(user.getSubject()), request.toNewTask());
    return ResponseEntity.created(URI.create("/api/tasks/" + task.id().value()))
            .body(TaskResponse.from(task));
}
```

```java
// no userId and no status field on purpose: owner = token, new tasks start TO_DO
public record CreateTaskRequest(@NotBlank @Size(max = 100) String title,
                                @Size(max = 2000) String description,
                                @NotNull LocalDate dueDate) {
}
```

```java
// ownership is part of the query, so someone else's task just isn't found
Optional<TaskEntity> findByIdAndOwnerUsername(Long id, String ownerUsername);
```

The tests read like the spec:

```java

@Test
@DisplayName("The one where Ana asks for Ben's task by id and gets 404, not 403")
void anotherUsersTaskLooksLikeItDoesNotExist() { ...}
```

For the real version of all this, see `BE/`. The same patterns are in `StoredPokemonController`,
`ApiExceptionHandler` and `SecurityConfig`.

## 3. How I validated it

- I approve the spec before any code gets written. If the rules are wrong, the code built on them will be too.
- Every test has to fail first, and for the right reason. A test that passes straight away counts as a broken test.
- An ArchUnit test (`ArchitectureTest`) breaks the build if the domain imports Spring or JPA.
- I run `/review` before each commit, read every diff (one rule per commit keeps them small), and use the
  `http/` request files to check the API against the app running in Docker.

## 4. What I had to correct

These all happened while I was building this repo:

- **Skipped the inner TDD loop.** After the acceptance test went red, it wrote a batch of production code
  straight away. I told it: *"You didn't create tdd failing tests first. Follow inner loop."*
- **DTOs nested inside the controller.** I had it move them to `adapter/in/web/dto`, one record per file.
- **Scope creep.** It offered to set up extra things I hadn't asked for, like an H2 test profile. I told it to
  do the task and stop.
- **Real bugs:** the React page went blank (a `useEffect` returned a Promise) and browsing was slow (about 10s per
  page). We fixed both: an error boundary per route, and parallel fetching on virtual threads,
  which brought it down to about 1s. See commit `70b149e`.

Claude Code saves corrections like these to its memory, so I didn't have to repeat them.

## 5. Edge cases, auth, validation

- **Auth:** stateless JWT, every `/api/tasks` route needs a token, expired tokens are rejected with zero clock
  skew. A missing or bad token always gets the same 401, and the reason it was rejected isn't revealed.
- **Ownership:** the user comes only from the token, never from the request body. Someone else's task returns **404, not
  403**, so nobody can probe ids to find out which tasks exist.
- **Validation:** Bean Validation on the DTOs, business rules in the domain. Both return the same 400 body with
  a list of the invalid fields. Bad enum values like `"FINISHED"` or a broken date give a clean 400, not a
  Jackson stack trace.
- **Edge cases that came up in `/tdd`:** due today is OK but yesterday isn't (dates come from an injected
  `Clock`, so tests don't depend on the real date); editing a task that's already overdue is allowed; PUT is a
  full replace; a `userId` sent in the body is ignored; page size is capped at 100.
