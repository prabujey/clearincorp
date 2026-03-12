import { Injectable } from "@angular/core";
import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import * as SockJS from "sockjs-client";
import { environment } from "src/environments/environment";
import { Subject, Observable } from "rxjs";

export interface WsEvent<T> {
  type: string;
  payload: T;
  refId?: number | string | null;
}

@Injectable({ providedIn: "root" })
export class WsService {
  private client: Client | null = null;
  private subscriptions = new Map<string, StompSubscription>();
  private eventSubject = new Subject<WsEvent<any>>();

  public readonly events$: Observable<WsEvent<any>> =
    this.eventSubject.asObservable();
 // loginUser: any;
  private pendingSubs: Array<{
    destination: string;
    callback: (event: WsEvent<any>) => void;
  }> = [];

  /**
   * Initialize STOMP over SockJS connection.
   */
   connect(): void {
    if (this.client && this.client.active) return;

    const wsUrl = (environment as any).wsBaseUrl || `${environment.apiBaseUrl.replace(/\/$/, '')}/ws`;

    this.client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 5000,
      debug: (str) => { /* console.log(str); */ }
    });

    this.client.onConnect = (frame) => {
      console.log('[WS] Connected');
      const queued = [...this.pendingSubs];
      this.pendingSubs = [];
      queued.forEach((s) => this.subscribe(s.destination, s.callback));
    };

    this.client.onStompError = (frame) => console.error('[WS] Stomp Error:', frame);
    this.client.activate();
  }

  /**
   * Subscribe to a specific WebSocket destination.
   * @param destination Destination path for WebSocket messages.
   * @param callback Function to handle incoming messages.
   */
 subscribe<T>(destination: string, callback: (event: WsEvent<T>) => void): void {
    if (!this.client) {
      this.connect();
    }

    if (!this.client?.connected) {
      this.pendingSubs.push({ destination, callback: callback as any });
      return;
    }

    if (this.subscriptions.has(destination)) return;

    const sub = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const payload = JSON.parse(message.body);
        const event: WsEvent<T> = {
          type: payload.type || 'message',
          payload: payload.payload || payload,
          refId: payload.refId || null
        };
        callback(event);
        this.eventSubject.next(event);
      } catch (e) {
        console.error('[WS] Parse error', e);
      }
    });

    this.subscriptions.set(destination, sub);
  }

  /**
   * Unsubscribe from a specific WebSocket destination.
   * @param destination Destination path to unsubscribe.
   */
  unsubscribeFromTopic(destination: string): void {
    const sub = this.subscriptions.get(destination);
    if (sub) {
      sub.unsubscribe();
      this.subscriptions.delete(destination);
    }
  }

  /**
   * Unsubscribe from all active topics.
   */
  unsubscribeAll(): void {
    this.subscriptions.forEach((subscription) => {
      subscription.unsubscribe();
    });
    this.subscriptions.clear();
    console.log("[WS] Unsubscribed from all topics");
  }

  /**
   * Send a message to a specific WebSocket destination.
   * @param destination Destination path for sending the message.
   * @param message Message payload to send.
   */
  // sendMessage(destination: string, message: any): void {
  //   if (!this.client || !this.client.connected) {
  //     console.error("[WS] Not connected");
  //     return;
  //   }

  //   const messageString = JSON.stringify(message);
  //   this.client.publish({ destination, body: messageString });
  //   console.log(`[WS] Sent message to ${destination}: ${messageString}`);
  // }

  /**
   * Disconnect the WebSocket connection.
   */
  disconnect(): void {
    if (this.client) {
      this.client.deactivate();
      console.log("[WS] WebSocket disconnected");
    }
  }
}
