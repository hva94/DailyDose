# Feature Specification: Daily Prompt Posting

**Feature Branch**: `004-daily-prompt-posting`  
**Created**: 2026-04-20  
**Status**: Draft  
**Input**: User description: "Feature Spec — Daily Prompt (Home-driven posting with dynamic title generation)"

## Overview

This feature adds one shared daily prompt to the home experience so posting feels more intentional and tied to a specific moment in the day. The prompt acts as a lightweight invitation to post, gives the creation flow a clear context, and produces a time-based default title that still respects the user's ability to edit it.

The feature adds value by giving both users the same daily creative nudge, making prompt-driven posts feel connected without turning the app into a streak or challenge system, and keeping the resulting post metadata readable in the feed.

## Scope

### In Scope

- Show one shared daily prompt card near the top of the home feed when the current user has not yet posted that day.
- Rotate one prompt-and-title combo per day for all users.
- Prevent the same combo from being used on consecutive days.
- Open the posting flow from the prompt card and carry the active prompt into that flow.
- Replace the default posting header with the active prompt during prompt-driven posting.
- Let the user optionally provide a short answer that can influence the default title.
- Generate a default post title that includes the publish-time timestamp and remains editable by the user.
- Store prompt context on prompt-driven posts so the feed can show the prompt, title, and image together.
- Hide the daily prompt card for a user after that user's first post of the day, regardless of whether later posts are created.

### Out of Scope

- Streaks, badges, achievements, or gamified completion tracking.
- Notifications, reminders, or prompt-based nudges outside the home feed.
- Multiple prompts in the same day.
- Public prompt archives, search, or prompt discovery surfaces.
- Restricting normal posting when no prompt is available or when the user chooses not to use the prompt.

## Non-Goals

- Turning posting into a mandatory daily challenge.
- Requiring every post to carry prompt context.
- Creating a separate social feed organized by prompts.
- Locking the user into a system-generated title.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See today's prompt in home (Priority: P1)

A signed-in user who has not posted yet today can see a daily prompt card near the top of the home feed so they know what today's shared posting cue is and can start posting from it.

**Why this priority**: The home prompt card is the entry point for the feature. Without it, users do not discover the daily prompt experience.

**Independent Test**: Open the home feed as a user with no post today and confirm the daily prompt card appears with the daily label, prompt text, and helper text. Post once, return to the feed, and confirm the card is hidden for that user.

**Acceptance Scenarios**:

1. **Given** a daily combo exists for the current day and the user has not posted today, **When** the user opens the home feed, **Then** the feed shows a `Daily Prompt` card near the top with the active prompt text and the helper text `Tap to post your daily dose`.
2. **Given** the user has already created at least one post today, **When** the user opens or refreshes the home feed, **Then** the daily prompt card is not shown for that user.
3. **Given** no daily prompt is available for the day, **When** the user opens the home feed, **Then** the feed does not show a prompt card and standard posting remains available.

---

### User Story 2 - Create a prompt-driven post (Priority: P1)

A user can tap the daily prompt, enter an optional short answer, add a photo, and publish a post whose default title includes the publish-time timestamp and stays editable.

**Why this priority**: Prompt-driven post creation is the core value of the feature and directly affects whether the prompt becomes a meaningful daily ritual.

**Independent Test**: Tap the daily prompt card, verify the same prompt appears in the posting flow, publish one post with no answer and one with an answer, and confirm each post receives the correct default title behavior with a timestamp.

**Acceptance Scenarios**:

1. **Given** the user taps the daily prompt card, **When** the posting flow opens, **Then** the active prompt is shown as the screen header and the flow offers an optional short-answer input plus photo selection.
2. **Given** the user leaves the short-answer input empty, **When** they publish a prompt-driven post, **Then** the product generates the default title from one of the active combo's title patterns and replaces `%time` with the actual publish-time label.
3. **Given** the user enters a short answer, **When** they publish a prompt-driven post, **Then** the product generates the default title using the answer-plus-time format and includes the actual publish-time label.
4. **Given** the user edits the generated title before confirming publish, **When** the post is saved, **Then** the edited title is used instead of replacing it with a new generated value.

---

### User Story 3 - Read prompt context in the feed (Priority: P2)

A user can recognize that a post came from the daily prompt because the feed shows the prompt context above the title and image, while long generated titles remain readable without losing any text.

**Why this priority**: Feed presentation makes the feature visible after posting and reinforces the idea that each prompt-driven post belongs to a shared daily moment.

**Independent Test**: View the feed after prompt-driven posts are published and confirm each such post shows prompt text, title, and image in order. Verify long titles can be expanded without truncating stored content.

**Acceptance Scenarios**:

1. **Given** a post was created through the daily prompt flow, **When** it appears in the feed, **Then** the feed shows the prompt context, the post title, and the image as a single card.
2. **Given** a prompt-driven title is longer than the default visible space, **When** the feed first renders the post, **Then** the title shows a reasonable preview with a subtle ellipsis affordance and can expand to reveal the full stored title.
3. **Given** a post was created without using the daily prompt, **When** it appears in the feed, **Then** it remains allowed and does not display prompt context that was never attached.

---

### User Story 4 - Keep prompt visibility personal and day-based (Priority: P2)

Each user independently sees or hides the daily prompt based on their own posting activity, while all users still receive the same active prompt on the same day.

**Why this priority**: The feature depends on a shared prompt without forcing all users into the same completion state.

**Independent Test**: Compare two users on the same day where one has posted and the other has not. Confirm they share the same prompt assignment for the day but only the user who has not posted still sees the card.

**Acceptance Scenarios**:

1. **Given** two users open the app on the same calendar day before either has posted, **When** they view the home feed, **Then** both see the same prompt text for that day.
2. **Given** one user has already posted today and another has not, **When** both users view the home feed, **Then** only the user without a post still sees the daily prompt card.
3. **Given** a user has already completed the prompt with their first post of the day, **When** they create additional posts that same day, **Then** the prompt card remains hidden.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The product MUST select one active daily prompt combo per calendar day from the defined prompt-and-title combo set.
- **FR-002**: The same active daily prompt combo MUST be shown to all users on the same calendar day.
- **FR-003**: The product MUST prevent the same daily prompt combo from being active on two consecutive calendar days.
- **FR-004**: If no daily prompt combo is available for a day, the product MUST omit the daily prompt card and continue allowing normal posting.
- **FR-005**: The home feed MUST show a `Daily Prompt` card near the top of the feed when the current user has not created any post that day and a daily combo is available.
- **FR-006**: The daily prompt card MUST display the active prompt text and the helper text `Tap to post your daily dose`.
- **FR-007**: The product MUST hide the daily prompt card for a user immediately after that user's first successfully published post of the day.
- **FR-008**: The first post of the day MUST count as prompt completion for card-visibility purposes even if that post was not created from the daily prompt flow.
- **FR-009**: Tapping the daily prompt card MUST open the post creation flow with the same prompt that was active in the home feed.
- **FR-010**: In prompt-driven posting, the creation screen header MUST show the active prompt instead of the default posting header text.
- **FR-011**: In prompt-driven posting, the product MUST offer an optional short-answer input separate from photo selection.
- **FR-012**: The user MUST be able to publish a prompt-driven post without entering a short answer.
- **FR-013**: The product MUST generate one default title per prompt-driven post and associate that title with the saved post.
- **FR-014**: Every system-generated default title MUST include a publish-time label representing the actual moment the post is published.
- **FR-015**: When the short-answer input is empty, the product MUST generate the default title by randomly selecting one title pattern from the active daily combo and replacing `%time` with the publish-time label.
- **FR-016**: When the short-answer input contains a value, the product MUST generate the default title using one of the answer-plus-time formats and include the publish-time label.
- **FR-017**: The publish-time label used in generated titles MUST match the app's existing simple, human-readable time style for posting.
- **FR-018**: Within a given combo, the product MUST vary prompt-only title generation by randomly selecting from the combo's available title patterns for each post.
- **FR-019**: The generated title MUST remain editable by the user before the post is finalized.
- **FR-020**: If the user manually edits the title before final confirmation, the product MUST preserve the user's edited title instead of replacing it with a newly generated one.
- **FR-021**: A prompt-driven post MUST retain the prompt context it was created from so the feed can show prompt, title, and image together.
- **FR-022**: The feed MUST show prompt context on posts created through the daily prompt flow.
- **FR-023**: The feed MUST continue to show title and image normally for posts that were not created through the daily prompt flow.
- **FR-024**: In the feed, prompt-driven posts MUST display prompt context, title, and image in that order.
- **FR-025**: If a generated or edited title exceeds the default visible title space in the feed, the product MUST preserve the full stored title and offer a subtle ellipsis-based way to reveal the remaining text.
- **FR-026**: The product MUST never silently truncate the saved title content.
- **FR-027**: A user MUST still be able to create posts outside the prompt flow, and those posts MUST remain valid even without attached prompt context.
- **FR-028**: If a user creates multiple posts in the same day, only the first post MUST determine completion of that day's prompt visibility state.
- **FR-029**: The daily prompt visibility state MUST be evaluated independently for each user based on that user's own posting activity.
- **FR-030**: Posts created through the daily prompt flow MUST remain readable even on later days after the active prompt changes.

### Key Entities *(include if feature involves data)*

- **Daily prompt combo**: A single reusable bundle containing one prompt text and the title patterns associated with that prompt.
- **Daily prompt assignment**: The calendar-day selection of one active combo that all users share for that day.
- **Prompt-driven post**: A user post created from the daily prompt flow that stores prompt context, title, image, and optional answer influence.
- **Prompt completion state**: The per-user, per-day outcome that determines whether the daily prompt card should still be visible.

## Edge Cases

- No combo is available for the day, so the home feed skips the prompt card and standard posting still works.
- The user opens the prompt flow before midnight and publishes after the day changes; the final title uses the actual publish-time label, and the saved post follows the day boundary used by the app for posting.
- The user leaves the answer blank; the post still receives a valid prompt-derived default title with a timestamp.
- The user enters a long answer; the stored title remains intact, while the feed uses a preview-plus-expand behavior instead of cutting off the data.
- The user edits the generated title to remove the timestamp or replace the full wording; the system accepts the manual title as the final user choice.
- The user creates a normal non-prompt post first; the daily prompt card is still considered completed and stays hidden for the rest of the day.
- The user creates multiple posts in one day; only the first post affects card visibility, and later posts do not make the card reappear.
- Older prompt-driven posts remain understandable in the feed even after the active daily prompt has rotated to a different combo.

## Acceptance Criteria

- [ ] A daily prompt combo is selected from the defined combo set for each day a prompt is available.
- [ ] All users see the same active prompt on the same day.
- [ ] The daily prompt card appears near the top of the home feed only for users who have not posted that day.
- [ ] Tapping the daily prompt card opens the post creation flow with the same prompt context.
- [ ] The prompt replaces the default creation header in prompt-driven posting.
- [ ] Users can optionally add a short answer before publishing.
- [ ] Publishing a prompt-driven post generates one default title using the defined rules and a publish-time label.
- [ ] System-generated default titles include a timestamp by default.
- [ ] Users can still edit or replace the generated title before final confirmation.
- [ ] After the user's first post of the day, the daily prompt card no longer appears for that user.
- [ ] The feed displays prompt, title, and image correctly for prompt-driven posts.
- [ ] Posts created without the prompt remain allowed and do not receive false prompt context.

## Rollout Notes

- Introduce the daily prompt as a lightweight enhancement to the existing home posting flow rather than a required step.
- Preserve the existing ability to post normally even when there is no active prompt or the user chooses another path.
- Keep prompt completion private to each user so one person's activity does not hide or reveal the card for someone else.
- Favor continuity in the feed: once a prompt-driven post is published, its prompt context remains visible as part of that post's story even after future daily prompts rotate.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In validation testing on any day with an active prompt, 100% of checked signed-in users see the same prompt text before they make their first post of the day.
- **SC-002**: In validation testing, 100% of users who publish their first post of the day no longer see the daily prompt card after the feed refreshes or is reopened.
- **SC-003**: In validation testing, 100% of prompt-driven posts save with one title, one image, and the correct prompt context when the prompt flow is used.
- **SC-004**: In title-generation checks, 100% of system-generated default titles include a publish-time label in the app's standard human-readable posting style.
- **SC-005**: In feed-display checks, 100% of prompt-driven posts show prompt context, title, and image in the correct order, and long titles can be expanded to reveal the full stored text.

## Assumptions

- The product already uses one consistent day boundary for deciding whether a user has posted "today," and this feature reuses that same boundary for prompt visibility and completion.
- The daily prompt combo set is managed as editorial content and contains only the prompt-and-title combinations defined for this feature unless expanded later.
- The short-answer input is intended to be optional and concise; users who want a different title can still edit the final title text before publishing.
- Manual title edits are treated as the user's final choice, even if the edit removes the default timestamp wording.
