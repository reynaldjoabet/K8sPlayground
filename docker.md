When you build a Docker image using a Dockerfile, each instruction (like `FROM`, `RUN`, `COPY`, etc.) creates a new layer.
A layer is basically a read-only snapshot of the filesystem after that command executes.
So, a Docker image is a stack of layers on top of each other, combined into a single filesystem view when the container runs

```yml
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y python3
COPY app/ /usr/src/app/
CMD ["python3", "/usr/src/app/main.py"]
```

This Dockerfile creates 4 layers:
- Base Layer → from ubuntu:22.04
- RUN Layer → installs Python
- COPY Layer → copies app files
- CMD Layer → sets default command (metadata layer)

Each layer is stored as a `compressed tar archive` of filesystem changes (called a “diff”).
Docker uses a `union filesystem` (like OverlayFS) to stack these diffs together into a single view.

Here’s how the layering system works conceptually:
```sh
Layer 4: CMD instruction (metadata)
Layer 3: COPY app files
Layer 2: RUN apt-get install python3
Layer 1: FROM ubuntu:22.04
----------------------------
Final Image = Combined view
```

When you start a container:
- Docker combines all layers into one unified filesystem.
- It adds a read-write layer on top, where changes made by the running container live.
This read-write layer disappears when the container stops (unless you use volumes)

## Layer Caching and Reuse
One of Docker’s biggest optimizations is layer caching:
When you rebuild an image, Docker checks if a layer has changed.
If not, it reuses the existing cached layer instead of rebuilding it.
This is why the order of commands matters.
For example:

```sh
RUN pip install -r requirements.txt
COPY . .
```
If you switch the order:

```sh
COPY . .
RUN pip install -r requirements.txt
```
then every time any file changes, Docker must rerun pip install, because the COPY invalidated the cache.


## Layer Sharing Across Images
Docker layers are content-addressed (identified by SHA256 hashes).
If multiple images use the same base (like ubuntu:22.04), they share those identical layers — saving disk space and download time.
Example:
Image A uses ubuntu:22.04 + Python
Image B uses ubuntu:22.04 + Node.js
Both share the same Ubuntu base layer.

## Types of Layers
There are two main categories:
Type	Description
- Image Layers	Read-only layers from the image itself. Built from Dockerfile instructions.
- Container Layer	The writable layer added when a container runs, storing runtime changes (deleted files, logs, etc.).

### How to Inspect Layers
You can view layers of an image using:
`docker history <image-name>`

Example output:
```sh
IMAGE          CREATED         CREATED BY                      SIZE
abc1234        2 hours ago     COPY app/ /usr/src/app/         12MB
def5678        2 hours ago     RUN apt-get install -y python3  150MB
ubuntu:22.04   1 week ago                                      77MB
```

## Best Practices for Efficient Layer Usage
- Minimize layers: Combine related commands with &&.
- Put least-changing steps first (to maximize caching).
- Clean up temporary files within the same RUN step.
- Use .dockerignore to exclude unnecessary files.
- Use multi-stage builds to avoid carrying build-time dependencies into final images

##### Image Layers
- Each image layer is read-only and stacked during the build process.
- They come from Dockerfile instructions (FROM, RUN, COPY, etc.).
- When the image is complete, those layers are frozen snapshots — immutable.
##### Container Layer
- The container layer is not part of the image.
- It’s created only when a container runs from an image.
- It’s a temporary, writable overlay on top of all the image layers.


Docker builds images layer by layer, and each layer depends on all the layers before it.
If you change a layer, Docker must rebuild that layer and all layers after it, because their cache becomes invalid

That’s why putting the steps that change the least first helps maximize caching — those earlier layers will rarely change, so Docker can reuse them on subsequent builds.

```sh
FROM python:3.11

COPY . /app
WORKDIR /app

RUN pip install -r requirements.txt

CMD ["python", "app.py"]

```

Every time any file in your project changes — even a README or test file —
the `COPY . /app` command changes the build context hash.
👉 That means Docker invalidates the cache for all subsequent layers — including the costly `pip install` step.
So `pip install` runs every single build, even if dependencies haven’t changed

Optimized Order — Least-Changing Steps First
Here’s the same Dockerfile, rewritten properly:
```sh
FROM python:3.11

WORKDIR /app

# Copy only dependency file first
COPY requirements.txt .

# Install dependencies (runs only when requirements.txt changes)
RUN pip install -r requirements.txt

# Copy application code (changes frequently)
COPY . .

CMD ["python", "app.py"]
```

When writing Dockerfiles:
- Base image first → rarely changes.
- Dependencies next → cache-friendly.
- Application code last → changes most often.


Each layer is stored locally on your system and identified by a unique hash (SHA256 digest) — that hash depends on:
- The instruction text itself,
- The files and directories it used (like copied files),
- And all layers below it.

You can inspect your cache layers using:
`docker history <image-name>`
Or clear the cache:
`docker builder prune`

A layer in Docker is created every time Docker executes a Dockerfile instruction that changes the filesystem — i.e., adds, deletes, or modifies files inside the image.
Each such instruction creates a read-only, immutable layer.
Docker stacks these layers on top of one another to form the full image

- `RUN, COPY, and ADD` create layers that increase the image size (filesystem layers).

- `WORKDIR, ENV, CMD, ENTRYPOINT, and LABEL `create layers that do not significantly increase the image size (metadata layers), but they still count as a layer for caching and can be seen with docker history.
Every `RUN` instruction creates a new layer.
Using `&& `allows you to combine multiple shell commands into a single `RUN `instruction, producing just one layer instead of multiple

A layer is a read-only snapshot of the filesystem that represents the result of a single Dockerfile instruction (like RUN, COPY, or ADD).
When Docker builds an image, each instruction adds a new filesystem diff (a layer) on top of the previous one

In Docker, each layer in an image represents a snapshot of a filesystem at that build step.

When Docker merges these, the container’s filesystem looks like one unified directory tree, even though it’s actually a stack of layers combined.

each layer captures the differences (changes) in the filesystem compared to the previous layer.
So a layer is not a complete copy of the whole filesystem —
it’s just a diff (the files that were added, modified, or deleted).


Think of it like version control (like Git commits):
- Each commit doesn’t store the whole project again — just the changes since the last commit.
- Docker layers work the same way for the filesystem

The base layer provides a root filesystem with /bin, /etc, /usr, /var, etc.
This is your starting point (a read-only snapshot of Ubuntu’s filesystem).

Each layer is a delta — the difference between the filesystem before and after that instruction.

Docker containers are just processes running on the host machine

So, from inside a container:
PID 1 looks like the first process,
but it’s really just another process on the host (maybe PID 5324).
You can’t see host processes because you’re in a different PID namespace.
Example:

```sh
# Inside container
ps -ef
```

You’ll see only your container’s processes —
but on the host:
`ps -ef | grep docker`
You’ll see all container processes listed as normal PIDs.

Cgroups control resource usage — CPU, memory, I/O, etc.
They ensure one container can’t hog all system resources.
Example:
`docker run -m 256m --cpus 1 ubuntu`
→ Kernel enforces memory and CPU limits for that container’s processes via cgroups.

`docker run -d --name web nginx`

What happens:
- Docker creates a new container from the nginx image.
- It launches a new process on the host:
/usr/sbin/nginx -g daemon off;
The kernel:
- Gives that process its own namespace view.
- Applies cgroup limits.
- Mounts the container’s filesystem.
- Connects it to a virtual network.

You can confirm with:
`ps aux | grep nginx`
You’ll see something like:
`root  2543  0.0  nginx: master process`
That’s your container’s process — running directly on your host.
```sh
docker run -d --name web nginx
351cba5d9589cb360a5b4d513588b56f735af4d30295643809376e924ec4984f
ps aux | grep nginx
username           63365   0.0  0.0 410724320   1392 s016  S+   12:21PM   0:00.00 grep nginx
```

```sh
docker top web
UID                 PID                 PPID                C                   STIME               TTY                 TIME                CMD
root                636                 615                 0                   08:21               ?                   00:00:00            nginx: master process nginx -g daemon off;
statd               673                 636                 0                   08:21               ?                   00:00:00            nginx: worker process
statd               674                 636                 0                   08:21               ?                   00:00:00            nginx: worker process
statd               675                 636                 0                   08:21               ?                   00:00:00            nginx: worker process
statd               676                 636                 0                   08:21               ?                   00:00:00            nginx: worker process
statd               677                 636                 0                   08:21               ?                   00:00:00            nginx: worker process
statd               678                 636                 0                   08:21               ?                   00:00:00            nginx: worker process
statd               679                 636                 0                   08:21               ?                   00:00:00            nginx: worker process
statd               680                 636                 0                   08:21               ?                   00:00:00            nginx: worker process
```

processes don’t own filesystems.
Filesystems exist in the kernel, not inside processes.
What a process has is a view of a filesystem — a namespace that tells it which files and directories it can see.

Using the mount namespace, Docker attaches the container’s filesystem as / for that process

By default (outside Docker, outside chroot, etc.), all your normal processes share one global filesystem namespace.
That’s why:
- If you create a file in one terminal with touch /tmp/abc,
- You can immediately see it in another terminal — because both processes see the same /tmp directory tree

Linux allows the kernel to create different mount namespaces,
so a process can see a different view of the filesystem.

the whole directory tree starting at /,is actually built from multiple filesystems mounted together.

```sh
docker exec -it web bash

root@351cba5d9589:/# mount | head
overlay on / type overlay (rw,relatime,lowerdir=/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/580/fs:/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/45/fs:/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/44/fs:/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/43/fs:/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/42/fs:/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/41/fs:/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/37/fs:/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/24/fs,upperdir=/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/581/fs,workdir=/var/lib/desktop-containerd/daemon/io.containerd.snapshotter.v1.overlayfs/snapshots/581/work)
proc on /proc type proc (rw,nosuid,nodev,noexec,relatime)
tmpfs on /dev type tmpfs (rw,nosuid,size=65536k,mode=755)
devpts on /dev/pts type devpts (rw,nosuid,noexec,relatime,gid=5,mode=620,ptmxmode=666)
sysfs on /sys type sysfs (ro,nosuid,nodev,noexec,relatime)
cgroup on /sys/fs/cgroup type cgroup2 (ro,nosuid,nodev,noexec,relatime)
mqueue on /dev/mqueue type mqueue (rw,nosuid,nodev,noexec,relatime)
shm on /dev/shm type tmpfs (rw,nosuid,nodev,noexec,relatime,size=65536k)
/dev/vda1 on /etc/resolv.conf type ext4 (rw,relatime,discard)
/dev/vda1 on /etc/hostname type ext4 (rw,relatime,discard)
```

```sh
overlay on / type overlay (rw,relatime,
lowerdir=/var/lib/desktop-containerd/.../snapshots/580/fs:...:/snapshots/24/fs,
upperdir=/var/lib/desktop-containerd/.../snapshots/581/fs,
workdir=/var/lib/desktop-containerd/.../snapshots/581/work)

```
```sh
Term	Meaning
overlay	The filesystem driver type (OverlayFS).
on /	It’s mounted as the container’s root /.
lowerdir	A colon-separated list of read-only layers (your image layers). These come from the base image, like Ubuntu or Python.
upperdir	The writable layer for this specific container. Any file you create or modify goes here.
workdir	Used internally by the kernel for OverlayFS operations (temporary workspace).
```

So:
- All the lowerdir paths = the image’s layers, stored by containerd.
The upperdir = your container’s writable layer (unique per container).
- OverlayFS merges these into a single unified filesystem that the process inside the container sees as /.

```sh
## Standard Virtual Filesystems
proc on /proc type proc (...)
tmpfs on /dev type tmpfs (...)
devpts on /dev/pts type devpts (...)
sysfs on /sys type sysfs (...)
cgroup on /sys/fs/cgroup type cgroup2 (...)
mqueue on /dev/mqueue type mqueue (...)
shm on /dev/shm type tmpfs (...)
```

These are bind mounts from the host into the container:
- /etc/resolv.conf → gives the container DNS configuration
- /etc/hostname → gives the container its hostname

```sh
root@351cba5d9589:/# cat /etc/resolv.conf
# Generated by Docker Engine.
# This file can be edited; Docker Engine will not make further changes once it
# has been modified.

nameserver 192.168.65.7

# Based on host file: '/etc/resolv.conf' (legacy)
# Overrides: []
```
```sh
# hostname is the container id
root@351cba5d9589:/# cat /etc/hostname
351cba5d9589
```

A namespace is a Linux kernel feature that isolates a particular kind of global resource so that one process (or group of processes) sees its own independent version of that resource.

```sh
FROM ubuntu:22.04
RUN mkdir -p /app/logs
```
our new image now permanently includes `/app/logs/`

```sh
FROM ubuntu:22.04
WORKDIR /app/data
```
If `/app/data` doesn’t exist yet, Docker automatically creates it.
No need for `RUN mkdir`


```sh
docker inspect nginx |jq
[
  {
    "Id": "sha256:fb39280b7b9eba5727c884a3c7810002e69e8f961cc373b89c92f14961d903a0",
    "RepoTags": [
      "nginx:latest"
    ],
    "RepoDigests": [
      "nginx@sha256:fb39280b7b9eba5727c884a3c7810002e69e8f961cc373b89c92f14961d903a0"
    ],
    "Parent": "",
    "Comment": "buildkit.dockerfile.v0",
    "Created": "2025-04-16T14:50:31Z",
    "DockerVersion": "",
    "Author": "",
    "Architecture": "arm64",
    "Variant": "v8",
    "Os": "linux",
    "Size": 68846241,
    "GraphDriver": {
      "Data": null,
      "Name": "overlayfs"
    },
    "RootFS": {
      "Type": "layers",
      "Layers": [
        "sha256:41d20f587704e5cf54b3319f41c96bc2463d350e3ffe2326d7643a488409936c",
        "sha256:c2284c4746eb7ed43539bf29fbce541b2dd2499da424b3a41d071f08b50bd23a",
        "sha256:b534cc4d04bf1e43093c150ff42337d75c85c78004ac7fa49e1df04b93575996",
        "sha256:d93efcb7b66f7a9deea3faff3f3c40d03ef963aa5441d38afe71d822488ef6a1",
        "sha256:1727cae455fd1e68906705a2816787611d0584d1a819bb99595eaf90ad2782d5",
        "sha256:cb61c9d607ef4850ea7b604a5b89849f2786634a601d900c5ebbfcd5495b3316",
        "sha256:3ef38621714a87c5fae2d1aac070c45aec8494ca6901b7e375b3694c6a78ee29"
      ]
    },
    "Metadata": {
      "LastTagTime": "2025-06-03T14:42:37.346353Z"
    },
    "Descriptor": {
      "mediaType": "application/vnd.oci.image.index.v1+json",
      "digest": "sha256:fb39280b7b9eba5727c884a3c7810002e69e8f961cc373b89c92f14961d903a0",
      "size": 10248
    },
    "Config": {
      "Cmd": [
        "nginx",
        "-g",
        "daemon off;"
      ],
      "Entrypoint": [
        "/docker-entrypoint.sh"
      ],
      "Env": [
        "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
        "NGINX_VERSION=1.27.5",
        "NJS_VERSION=0.8.10",
        "NJS_RELEASE=1~bookworm",
        "PKG_RELEASE=1~bookworm",
        "DYNPKG_RELEASE=1~bookworm"
      ],
      "ExposedPorts": {
        "80/tcp": {}
      },
      "Labels": {
        "maintainer": "NGINX Docker Maintainers <docker-maint@nginx.com>"
      },
      "OnBuild": null,
      "StopSignal": "SIGQUIT",
      "User": "",
      "Volumes": null,
      "WorkingDir": ""
    }
  }
]
```

Each layer in an image contains a set of filesystem changes - additions, deletions, or modifications

In the `RootfFS.Layers` section of the `docker inspect` output, you can see the SHA256 hashes of the layers that make up the image. These hashes correspond to the directories in the `/var/lib/Docker/overlay2` directory on your machine

If we wanted to manually inspect them, we could do so by navigating to the directory and listing the contents.

```sh
cd /var/lib/docker/overlay2/53f4a1cb375f9a65b31a763f71c663de37193ff63bd336c04006d14f839cb7a9
ls
committed  diff  link  lower  work
```

Notice each of these layers contains a `diff` directory. This is where the filesystem changes for that layer are stored. The `lower` directory contains the layers below it in the stack, and the `upper` directory contains the writable layer for the container.
### Stacking the layers

Layering is made possible by content-addressable storage and union filesystems. While this will get technical, here’s how it works:

- After each layer is downloaded, it is extracted into its own directory on the host filesystem.
- When you run a container from an image, a union filesystem is created where layers are stacked on top of each other, creating a new and unified view.
- When the container starts, its root directory is set to the location of this unified directory, using `chroot`

When the union filesystem is created, in addition to the image layers, a directory is created specifically for the running container. This allows the container to make filesystem changes while allowing the original image layers to remain untouched. This enables you to run multiple containers from the same underlying image


Each layer only stores the differences (the "diff") from the layer before it.

Docker image layers implement a union filesystem. Each layer includes only the changes its build stage made, but unionization combines an image’s layers to produce a single logical filesystem. This occurs transparently during container runtime.

Docker image layers are always immutable. Once they’re created, they’re read-only and cannot be modified. Any changes, such as adding or removing files, must be applied in a new layer.

Immutability raises a problem when starting containers. The container may need to write new content to its filesystem, such as temporary files created by the application. 

To solve this issue, Docker adds a transient read-write layer to the top of the union filesystem’s layer stack. This allows the container to write into the filesystem seamlessly, but changes are lost when the container is stopped

image registries only need to store the new layers they don’t already have. When users pull an image from a registry, they download only the layers that are missing from their local cache


Each layer is only a set of differences from the layer before it. Note that both adding, and removing files will result in a new layer.
```sh
| Command         | Creates New Layer? | Description                                                         |
| --------------- | ------------------ | ------------------------------------------------------------------- |
| `docker build`  | ✅                  | Builds an image, each relevant Dockerfile instruction adds a layer. |
| `docker commit` | ✅                  | Saves the current state of a container as a new image layer.        |

```
The `actions/checkout` is already reasonably optimized by default, because it performs a shallow clone with `depth=1`.

A `depth=1` means git checks out the current `HEAD` commit and only its contents. This means it cannot go to a different commit in history.

your file desciptors start at 3..0 is standard input(keyboard),1 is standard output(terminal) and 2 is standard error(which can also be terminal)

When a process opens a file, linux makes an entry in the file descriptor table

A file descriptor (FD) is a low-level integer handle used by an operating system to identify and manage open files or I/O resources within a process.

When a program opens a file (using open(), fopen(), etc.), the operating system creates an entry in a table of open files and returns an integer — the file descriptor.
That integer uniquely identifies the open file within the process

`write(1, "Hello, world\n", 13);`

This writes directly to standard output (file descriptor 1).

When a file is opened:
- The OS checks if the process has permission to access it.
- It creates a kernel object representing the file.
- The process gets a file descriptor number referring to that kernel object.
Multiple file descriptors (even across processes) can refer to the same open file

the memory management unit(MMu) maps the virtual address to physical memory


virtual memory is organized into fixed sized blocks called pages


VPC is scoped per region and subnets within it are within availability zones

A public subnet is connected to an internet gateway

Each Docker image layer is a directory on the host filesystem.
Inside that directory are the files and folders that were created or changed during one Dockerfile instruction