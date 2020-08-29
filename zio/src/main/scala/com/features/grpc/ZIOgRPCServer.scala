package com.features.grpc

import com.features.zio.connectorManager.{ConnectorInfoDTO, ConnectorManagerGrpc, ResponseInfo}
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}
import zio.Runtime.{default => Main}
import zio.{Has, Task, ULayer, ZIO, ZLayer}

import scala.concurrent.{ExecutionContext, Future}

object ZIOgRPCServer extends App {

  private val port = 9999

  /**
   * Description of ConnectorManager dependency
   */
  trait ConnectorManager {
    def getConnectorManager: Task[ConnectorManagerGrpc.ConnectorManager]
  }

  trait GRPCServer {
    def getServer(service: ServerServiceDefinition): Task[Server]
  }

  /**
   * Implementation/Behavior of ZLayer as dependency to be passed to the program.
   */
  val connectorManagerDependency: ULayer[Has[ConnectorManager]] = ZLayer.succeed(new ConnectorManager {
    override def getConnectorManager: Task[ConnectorManagerGrpc.ConnectorManager] = ZIO.succeed {
      (connector: ConnectorInfoDTO) => {
        val reply = ResponseInfo(message = s"Some logic ${connector.requestInfo} of ${connector.connectorName}")
        Future.successful(reply)
      }
    }
  })

  val serverDependency = ZLayer.succeed(new GRPCServer {
    override def getServer(service: ServerServiceDefinition): Task[Server] = {
      ZIO.effect(ServerBuilder.forPort(port)
        .addService(service)
        .asInstanceOf[ServerBuilder[_]]
        .build)
    }
  })

  /**
   * DSL/Structure of how to obtain the Connector manager dependency inside the program
   *
   * @return
   */
  def getConnectorManager: ZIO[Has[ConnectorManager], Throwable, ConnectorManagerGrpc.ConnectorManager] =
    ZIO.accessM(_.get.getConnectorManager)

  def getGRPCServer(service: ServerServiceDefinition): ZIO[Has[GRPCServer], Throwable, Server] =
    ZIO.accessM(_.get.getServer(service))

  /**
   * Server program that receive the ConnectorManager as dependency, it is bind in the service
   */
  private val serverProgram: ZIO[Has[ConnectorManager] with Has[GRPCServer], Throwable, Unit] =
    (for {
      connectorManager <- getConnectorManager
      service <- ZIO.effect(ConnectorManagerGrpc.bindService(connectorManager, ExecutionContext.global))
      server <- getGRPCServer(service)
      _ <- ZIO.effect(server.start())
      _ <- ZIO.succeed(println(s"ZIO gRPC server running on port $port"))
      _ <- ZIO.effect(server.awaitTermination())
    } yield ()).catchAll(t => {
      println(s"Error running ZIO gRPC server. Caused by $t")
      ZIO.fail(t)
    })

  val multiDependency: ZLayer[Any, Any, Has[ConnectorManager] with Has[GRPCServer]] =
    connectorManagerDependency ++ serverDependency

  Main.unsafeRun(serverProgram.provideCustomLayer(multiDependency))

}


