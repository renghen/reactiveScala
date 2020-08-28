package com.features.grpc

import com.features.zio.connectorManager.{ConnectorInfoDTO, ConnectorManagerGrpc, ResponseInfo}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ZIOgRPCClient extends App {

  val connectorManagerStub = ConnectorManagerGrpc.stub(createChannel)
  val request = ConnectorInfoDTO(connectorName = "Rest", requestInfo = "READ")
  val reply: Future[ResponseInfo] = connectorManagerStub.connectorRequest(request)
  println(Await.result(reply, 10 seconds).message)

  private def createChannel = {
    val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 9999)
      .usePlaintext()
      .asInstanceOf[ManagedChannelBuilder[_]]
      .build()
    channel
  }

}


