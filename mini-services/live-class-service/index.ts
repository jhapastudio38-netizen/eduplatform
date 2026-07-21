/**
 * Live class WebSocket service — port 3003.
 *
 * Rooms keyed by live class roomCode. Teacher joins as host, students join
 * with the code. Supports chat messages + attendee count broadcasting.
 *
 * Scaling to 100k concurrent users: front this with a Redis adapter
 * (`@socket.io/redis-adapter`) so multiple Node processes share state.
 */
import { createServer } from "http";
import { Server } from "socket.io";

const httpServer = createServer();
const io = new Server(httpServer, {
  path: "/",
  cors: { origin: "*", methods: ["GET", "POST"] },
  pingTimeout: 60000,
  pingInterval: 25000,
});

const rooms = new Map<string, Set<string>>(); // roomCode -> set of socket ids

function broadcastCount(room: string) {
  const set = rooms.get(room);
  const n = set ? set.size : 0;
  io.to(room).emit("attendee_count", n);
}

io.on("connection", (socket) => {
  console.log(`[live] connected ${socket.id}`);

  socket.on("join", ({ role, room }: { role: "teacher" | "student"; room: string }) => {
    if (!room) return;
    socket.join(room);
    socket.data.room = room;
    socket.data.role = role;

    if (!rooms.has(room)) rooms.set(room, new Set());
    rooms.get(room)!.add(socket.id);
    broadcastCount(room);
    console.log(`[live] ${role} joined room ${room} (total: ${rooms.get(room)!.size})`);
  });

  socket.on("chat", ({ from, msg }: { from: string; msg: string }) => {
    const room = socket.data.room;
    if (!room) return;
    // Basic server-side input sanity
    const safeMsg = String(msg).slice(0, 1000);
    const safeFrom = String(from).slice(0, 60);
    io.to(room).emit("chat", { from: safeFrom, msg: safeMsg });
  });

  socket.on("disconnect", () => {
    const room = socket.data.room;
    if (room && rooms.has(room)) {
      rooms.get(room)!.delete(socket.id);
      if (rooms.get(room)!.size === 0) rooms.delete(room);
      else broadcastCount(room);
    }
    console.log(`[live] disconnected ${socket.id}`);
  });

  socket.on("error", (err) => console.error(`[live] socket error`, err));
});

const PORT = 3003;
httpServer.listen(PORT, () => {
  console.log(`[live] WebSocket server running on port ${PORT}`);
});

process.on("SIGTERM", () => httpServer.close(() => process.exit(0)));
process.on("SIGINT", () => httpServer.close(() => process.exit(0)));
