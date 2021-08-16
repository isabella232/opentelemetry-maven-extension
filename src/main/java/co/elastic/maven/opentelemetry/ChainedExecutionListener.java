/*
 * Copyright The Original Author or Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package co.elastic.maven.opentelemetry;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class ChainedExecutionListener implements ExecutionListener {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Collection<ExecutionListener> listeners;

    public ChainedExecutionListener() {
        this.listeners = new LinkedList<>();
    }

    public ChainedExecutionListener(ExecutionListener... listeners) {
        this.listeners = new LinkedList<>(Arrays.asList(listeners));
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.projectDiscoveryStarted(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.sessionStarted(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.sessionEnded(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.projectSkipped(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.projectStarted(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.projectSucceeded(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.projectFailed(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.mojoSkipped(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.mojoStarted(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.mojoSucceeded(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.mojoFailed(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.forkStarted(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.forkSucceeded(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.forkFailed(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.forkedProjectStarted(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.forkedProjectSucceeded(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        for (ExecutionListener listener : this.listeners) {
            try {
                listener.forkedProjectFailed(event);
            } catch (RuntimeException e) {
                logger.error("Silently skip exception", e);
            }
        }
    }
}
