package beans

import java.util.UUID

case class RequestStatus(requestedId: String) {
  val requestId = UUID.randomUUID()
}