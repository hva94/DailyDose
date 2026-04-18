# Spec: Reactions and Replies Upgrade for Snapshots

**Feature Branch**: `003-snapshot-reactions-replies`  
**Created**: 2026-04-17  
**Status**: Draft  
**Input**: User description: "Create a product spec for upgrading the app from the current post model to the target model, introducing private reactions on posts, replies on posts, and summary fields on the main post document for faster display."

## Overview

This upgrade expands snapshot posts from a single like interaction into two clearer social actions: emoji reactions and text replies. It also adds summary values on each post so the feed can show reaction and reply activity without requiring the user to open the full conversation first.

The change adds value by giving the two regular users a more expressive way to respond to posts, a lightweight way to talk about a post in context, and faster visibility into which posts have activity. Existing posts remain visible and usable throughout the transition, even when newer summary fields are not present yet.

## Scope

### In Scope

- Replace the current like interaction with a single-reaction-per-user emoji reaction model on posts.
- Allow users to add, change, and remove their own reaction on a post.
- Show aggregate reaction totals on posts without exposing which user chose which emoji.
- Allow users to open a post's replies from the feed and add replies to that post.
- Show reply counts in the feed and in the post reply experience.
- Define how older posts behave when new reaction and reply summary fields are missing.
- Define how legacy `likeList` data should be treated so the experience has one consistent interaction model.

### Out of Scope

- Editing or deleting replies after they are posted.
- Reply threads, nested replies, mentions, or notifications.
- Public reaction identity lists, reactor profiles, or a full "who reacted" view.
- New moderation features, blocking tools, or reporting flows.
- Changes to photo upload, authentication, or the core feed ranking/order.

## Non-Goals

- Reintroducing likes as a separate interaction alongside reactions.
- Turning replies into a long-form chat experience.
- Requiring a full data backfill before the feature can be safely shown to users.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - React to a post with an emoji (Priority: P1)

A signed-in user can react to any visible post with one emoji so they can respond quickly without writing a reply.

**Why this priority**: Reactions are the direct replacement for the current like behavior and are the fastest way to add value to the existing feed.

**Independent Test**: Open a post from the feed, add an emoji reaction, and confirm the post reflects the user's selection and updated totals without needing any reply functionality.

**Acceptance Scenarios**:

1. **Given** a post with no reaction from the current user, **When** the user selects an emoji reaction, **Then** that emoji becomes the user's active reaction for the post and the post's reaction totals update.
2. **Given** a post already has reactions from other users, **When** the current user adds a reaction, **Then** the aggregate total increases by one and the summary reflects the selected emoji.

---

### User Story 2 - Change or remove my reaction (Priority: P1)

A signed-in user can toggle off the same emoji or replace it with a different emoji so their reaction state always matches their current intent.

**Why this priority**: A one-reaction model is only complete if the user can correct or clear their own input without friction.

**Independent Test**: React to a post, tap the same emoji again to remove it, then react again with a different emoji and confirm counts remain accurate after each action.

**Acceptance Scenarios**:

1. **Given** the user already reacted to a post with an emoji, **When** they select that same emoji again, **Then** their reaction is removed and the total reaction count decreases by one.
2. **Given** the user already reacted to a post with one emoji, **When** they select a different emoji, **Then** the old reaction is replaced, the total reaction count does not increase, and the summary moves one count from the old emoji to the new emoji.

---

### User Story 3 - View reaction totals on a post (Priority: P2)

A user can see how much reaction activity a post has and which emojis are being used, without seeing which person used each emoji.

**Why this priority**: Aggregate visibility makes reactions useful at the feed level while preserving the private nature of the interaction.

**Independent Test**: View a post with multiple reactions and confirm the feed or post interaction surface shows total reaction count plus emoji totals, but no user list.

**Acceptance Scenarios**:

1. **Given** a post has active reactions, **When** the user views it in the feed, **Then** the post shows the total number of reactions and a compact emoji summary.
2. **Given** a post has no active reactions, **When** the user views it, **Then** the post does not show misleading reaction totals and the user can still add the first reaction.

---

### User Story 4 - Open a post's replies (Priority: P2)

A user can open a dedicated reply view for a post so they can read the conversation in order and decide whether to participate.

**Why this priority**: Replies are only meaningful if users can reliably access the conversation from the feed.

**Independent Test**: Open the replies for a post from the feed and confirm the user sees the selected post context plus the current replies in time order.

**Acceptance Scenarios**:

1. **Given** a post has replies, **When** the user opens replies from the feed, **Then** a dedicated reply view opens and shows replies in chronological order from oldest to newest.
2. **Given** a post has no replies, **When** the user opens replies, **Then** the reply view shows an empty state that invites the user to add the first reply.

---

### User Story 5 - Add a reply to a post (Priority: P2)

A signed-in user can post a reply so they can comment directly on a snapshot without creating a new post.

**Why this priority**: Posting replies is the primary value of the new conversation capability.

**Independent Test**: Open a post's reply view, submit a valid reply, and confirm the new reply appears in chronological order and the post's reply count increases.

**Acceptance Scenarios**:

1. **Given** the user is viewing a post's replies, **When** they submit non-blank reply text within the allowed length, **Then** the reply is added to the conversation and the reply count increases by one.
2. **Given** the user attempts to submit a blank reply, **When** they tap send, **Then** the reply is not posted and the user receives clear guidance to enter text.

---

### User Story 6 - View reply counts in the feed (Priority: P3)

A user can see whether a post has an active conversation before opening it.

**Why this priority**: Feed-level reply counts improve discoverability, but the feature still provides value without them if the reply view exists.

**Independent Test**: View the feed and confirm each post shows an accurate reply count, including a clear zero-reply state for posts with no conversation yet.

**Acceptance Scenarios**:

1. **Given** a post has one or more replies, **When** the user views it in the feed, **Then** the feed shows the current reply count for that post.
2. **Given** a post has no replies, **When** the user views it in the feed, **Then** the feed shows a clear zero-reply state or prompt without implying missing data.

## Requirements *(mandatory)*

### Product Behavior

- **FR-001**: The product MUST replace the current like interaction with emoji reactions as the single post feedback mechanic shown to users.
- **FR-002**: A user MUST be able to have at most one active reaction per post at any time.
- **FR-003**: Selecting an emoji on a post with no existing reaction from the current user MUST create that user's active reaction for the post.
- **FR-004**: Selecting the same emoji again MUST remove the user's active reaction from the post.
- **FR-005**: Selecting a different emoji MUST replace the user's previous reaction rather than add a second reaction.
- **FR-006**: `reactionCount` MUST represent the total number of active reactions on the post.
- **FR-007**: `reactionSummary` MUST represent aggregate totals by emoji for all active reactions on the post.
- **FR-008**: Reaction displays MUST remain private at the identity level: users may see totals by emoji and their own selected emoji, but not which specific user chose each emoji.
- **FR-009**: `replyCount` MUST represent the total number of replies on the post.
- **FR-010**: Replies MUST be presented in chronological order from oldest to newest, with newly added replies appearing at the end.
- **FR-011**: Users MUST be able to open a dedicated reply view from a post in the feed.
- **FR-012**: Users MUST be able to add a reply from that reply view.
- **FR-013**: Reply text MUST require at least one non-whitespace character to be valid.
- **FR-014**: Reply text MUST be capped at 280 characters so replies stay concise and readable.
- **FR-015**: The feed MUST surface summary activity for each post using the main post document values so users can see reaction and reply activity without opening the full reply view.

### Migration Behavior

- **FR-016**: Existing posts MUST remain visible and usable even when `replyCount`, `reactionCount`, or `reactionSummary` are missing.
- **FR-017**: When new summary fields are missing on an older post, the product MUST treat `reactionCount` as `0`, `replyCount` as `0`, and `reactionSummary` as empty until migrated values become available.
- **FR-018**: When `reactionSummary` is empty or missing, the product MUST not show placeholder emoji totals that imply activity.
- **FR-019**: The legacy `likeList` MUST no longer be shown to users as a separate like concept anywhere the new reactions experience is available.
- **FR-020**: Historical `likeList` data SHOULD be treated as legacy migration input for a default heart reaction so prior engagement can be preserved where possible, but the user-facing product MUST still behave coherently before that migration is complete.
- **FR-021**: Before any backfill reaches an older post, the product MUST favor a clean "no reactions yet" display over showing both likes and reactions at the same time.

### UX Expectations

- **FR-022**: In the feed, each post MUST show reply count and, when reactions exist, a compact reaction summary with the total reaction count.
- **FR-023**: In the feed, posts with zero reactions MUST avoid visual clutter by hiding empty emoji totals while still allowing the user to add the first reaction.
- **FR-024**: In the feed, posts with zero replies MUST show a clear zero state or call to action that makes it obvious the user can start the conversation.
- **FR-025**: Opening replies MUST present the selected post context, the reply list, and a reply composer in the same user flow so the user does not lose context.
- **FR-026**: When a post has no replies, the reply view MUST show a friendly empty state that invites the first reply.
- **FR-027**: While reply or reaction data is loading, the product MUST show a deliberate loading state instead of blank or shifting content.
- **FR-028**: If reply or reaction data cannot be loaded, the product MUST keep the post view usable and show a clear error state with a retry path where the user can reasonably try again.
- **FR-029**: The feature MUST follow the app's existing offline experience expectations: previously available posts stay viewable, last known counts remain visible when available, and locally initiated reaction or reply actions made offline should appear as pending until the app reconnects.
- **FR-030**: If an offline reaction or reply cannot be confirmed after reconnecting, the product MUST restore the last confirmed state and explain that the action did not complete.
- **FR-031**: If a reply author's name is unavailable, the reply MUST remain visible and show a neutral fallback name.
- **FR-032**: If a reply author's photo is unavailable, the reply MUST remain visible and show a default avatar placeholder.

### Key Entities *(include if feature involves data)*

- **Snapshot post**: A photo post in the feed that now carries summary activity values for reactions and replies.
- **Reaction**: A user's single active emoji response on a post, visible only as an aggregate total to other users.
- **Reaction summary**: The post-level aggregate of active reactions grouped by emoji for feed and post display.
- **Reply**: A short text response attached directly to a snapshot post, shown in time order with lightweight author identity.

## Edge Cases

- Removing the only existing reaction on a post returns the post to a zero-reaction state and clears any empty emoji entry from the summary.
- Switching from one emoji to another changes the emoji totals without changing the overall `reactionCount`.
- A post with an empty or missing reaction summary shows no emoji totals, not a broken or placeholder state.
- A reply containing only spaces, tabs, or line breaks cannot be submitted.
- A reply longer than 280 characters cannot be submitted; the user is prompted to shorten it before posting.
- Posts with zero replies or zero reactions remain fully usable and invite the first interaction.
- If profile name or photo data is unavailable for a reply, the reply still appears with neutral fallback identity treatment.
- Older posts that have not been backfilled yet still open normally and default to zero summary values.

## Acceptance Criteria

- [ ] A user can add an emoji reaction to a post from the feed or post interaction surface.
- [ ] A user cannot hold more than one active reaction on the same post.
- [ ] Tapping the same emoji twice removes the user's reaction.
- [ ] Choosing a different emoji replaces the previous reaction without increasing `reactionCount`.
- [ ] Users can see aggregate reaction totals on a post without seeing which user chose each emoji.
- [ ] The feed shows reply counts for posts and updates the count after a successful new reply.
- [ ] Opening replies shows the selected post context and the replies in oldest-to-newest order.
- [ ] A user can add a valid reply, and blank replies are blocked.
- [ ] Posts with zero replies and zero reactions show clear empty states rather than broken or ambiguous UI.
- [ ] Existing posts remain visible and interactive even when new summary fields are not present yet.
- [ ] The app does not show likes and reactions as two separate concepts during the rollout.
- [ ] Offline use preserves a coherent experience with last known values and clear pending or failure feedback for local actions.

## Rollout Notes

- Introduce reactions as the replacement for likes in the user experience from day one of the rollout so users only learn one post feedback model.
- Treat reply support as additive: users can continue browsing existing posts even if a post has never received a reply or has not been backfilled with summary values yet.
- Accept that some older posts may briefly appear with zero reactions before legacy likes are migrated, but avoid showing a separate like UI during that period.
- Preserve trust during rollout by ensuring missing summary data produces calm zero states, not errors or disappearing posts.
- For this private two-user app, the rollout should feel low-friction and reversible at the product level: no post should become unreadable, and no user should need to relearn core feed navigation just to access reactions or replies.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In validation testing, both regular users can add, change, or remove a reaction on a post in no more than 2 taps after opening the reaction control.
- **SC-002**: In at least 95% of validation checks, feed-level reaction and reply summaries match the counts shown when the same post is opened in its reply view.
- **SC-003**: In 100% of rollout validation checks, older posts without new summary values remain visible, openable, and usable without showing a blocking error.
- **SC-004**: In usability validation, both regular users can identify whether a post has reactions or replies directly from the feed without opening more than one additional screen.

## Assumptions

- The app remains a private app with two regular signed-in users, so the product can optimize for clarity and low friction over large-scale social discovery patterns.
- The existing authentication and post viewing flows remain unchanged unless this feature explicitly adds reaction or reply behavior to them.
- The current offline support pattern should continue to apply to this feature at the product level, including preserving last known content and handling reconnect recovery gracefully.
- Reactions are intended to replace likes rather than coexist with them as separate concepts.
