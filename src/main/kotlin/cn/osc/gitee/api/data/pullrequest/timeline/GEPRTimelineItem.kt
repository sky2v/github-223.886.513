// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cn.osc.gitee.api.data.pullrequest.timeline

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.intellij.collaboration.ui.codereview.timeline.TimelineItem
import cn.osc.gitee.api.data.GEIssueComment
import cn.osc.gitee.api.data.pullrequest.GEPullRequestCommitShort
import cn.osc.gitee.api.data.pullrequest.GEPullRequestReview
import cn.osc.gitee.api.data.pullrequest.timeline.GEPRTimelineItem.Unknown

/*REQUIRED
IssueComment
PullRequestCommit (Commit in GEE)
PullRequestReview

RenamedTitleEvent
ClosedEvent | ReopenedEvent | MergedEvent
AssignedEvent | UnassignedEvent
LabeledEvent | UnlabeledEvent
ReviewRequestedEvent | ReviewRequestRemovedEvent
ReviewDismissedEvent
ReadyForReviewEvent

BaseRefChangedEvent | BaseRefForcePushedEvent
HeadRefDeletedEvent | HeadRefForcePushedEvent | HeadRefRestoredEvent

//comment reference
CrossReferencedEvent
//issue will be closed
ConnectedEvent | DisconnectedEvent
*/

/*MAYBE
LockedEvent | UnlockedEvent
MarkedAsDuplicateEvent | UnmarkedAsDuplicateEvent
ConvertToDraftEvent

???PullRequestCommitCommentThread
???PullRequestReviewThread
AddedToProjectEvent
ConvertedNoteToIssueEvent
RemovedFromProjectEvent
MovedColumnsInProjectEvent

TransferredEvent
UserBlockedEvent

PullRequestRevisionMarker

DeployedEvent
DeploymentEnvironmentChangedEvent
PullRequestReviewThread
PinnedEvent | UnpinnedEvent
SubscribedEvent | UnsubscribedEvent
MilestonedEvent | DemilestonedEvent
AutomaticBaseChangeSucceededEvent | AutomaticBaseChangeFailedEvent
 */
/*IGNORE
ReferencedEvent
MentionedEvent
CommentDeletedEvent
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = true,
              defaultImpl = Unknown::class)
@JsonSubTypes(
  JsonSubTypes.Type(name = "IssueComment", value = GEIssueComment::class),
  JsonSubTypes.Type(name = "PullRequestCommit", value = GEPullRequestCommitShort::class),
  JsonSubTypes.Type(name = "PullRequestReview", value = GEPullRequestReview::class),

  JsonSubTypes.Type(name = "ReviewDismissedEvent", value = GEPRReviewDismissedEvent::class),
  JsonSubTypes.Type(name = "ReadyForReviewEvent", value = GEPRReadyForReviewEvent::class),
  /*JsonSubTypes.Type(name = "ConvertToDraftEvent", value = GEPRConvertToDraftEvent::class),*/

  JsonSubTypes.Type(name = "RenamedTitleEvent", value = GEPRRenamedTitleEvent::class),

  JsonSubTypes.Type(name = "ClosedEvent", value = GEPRClosedEvent::class),
  JsonSubTypes.Type(name = "ReopenedEvent", value = GEPRReopenedEvent::class),
  JsonSubTypes.Type(name = "MergedEvent", value = GEPRMergedEvent::class),

  JsonSubTypes.Type(name = "AssignedEvent", value = GEPRAssignedEvent::class),
  JsonSubTypes.Type(name = "UnassignedEvent", value = GEPRUnassignedEvent::class),

  JsonSubTypes.Type(name = "LabeledEvent", value = GEPRLabeledEvent::class),
  JsonSubTypes.Type(name = "UnlabeledEvent", value = GEPRUnlabeledEvent::class),

  JsonSubTypes.Type(name = "ReviewRequestedEvent", value = GEPRReviewRequestedEvent::class),
  JsonSubTypes.Type(name = "ReviewRequestRemovedEvent", value = GEPRReviewUnrequestedEvent::class),

  JsonSubTypes.Type(name = "BaseRefChangedEvent", value = GEPRBaseRefChangedEvent::class),
  JsonSubTypes.Type(name = "BaseRefForcePushedEvent", value = GEPRBaseRefForcePushedEvent::class),

  JsonSubTypes.Type(name = "HeadRefDeletedEvent", value = GEPRHeadRefDeletedEvent::class),
  JsonSubTypes.Type(name = "HeadRefForcePushedEvent", value = GEPRHeadRefForcePushedEvent::class),
  JsonSubTypes.Type(name = "HeadRefRestoredEvent", value = GEPRHeadRefRestoredEvent::class),

  JsonSubTypes.Type(name = "CrossReferencedEvent", value = GEPRCrossReferencedEvent::class)/*,
  JsonSubTypes.Type(name = "ConnectedEvent", value = GEPRConnectedEvent::class),
  JsonSubTypes.Type(name = "DisconnectedEvent", value = GEPRDisconnectedEvent::class)*/
)
interface GEPRTimelineItem : TimelineItem {
  class Unknown(val __typename: String) : GEPRTimelineItem

  companion object {
    val IGNORED_TYPES = setOf("ReferencedEvent", "MentionedEvent", "CommentDeletedEvent")
  }
}